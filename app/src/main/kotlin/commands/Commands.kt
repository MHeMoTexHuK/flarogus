package flarogus.commands

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import javax.imageio.*;
import kotlin.random.*;
import kotlin.time.*
import kotlin.math.*
import kotlinx.coroutines.*;
import kotlinx.coroutines.flow.*;
import dev.kord.rest.builder.message.create.*
import dev.kord.core.event.*
import dev.kord.core.event.message.*
import dev.kord.core.supplier.*;
import dev.kord.core.entity.*;
import dev.kord.core.entity.channel.*
import dev.kord.core.behavior.*
import dev.kord.core.behavior.channel.*
import dev.kord.common.entity.*
import flarogus.*
import flarogus.util.*;
import flarogus.commands.*;
import flarogus.commands.Command as CustomCommand
import flarogus.commands.impl.*;
import flarogus.multiverse.*

fun initCommands() {
	CommandHandler.apply {
		register(RunCommand)

		register(MultiverseCommand)

		@OptIn(ExperimentalTime::class)
		register("sus") {
			val msg = message.replyWith("sussificating").await()
			msg?.edit {
				val ping = message.id.timeMark.elapsedNow().toLong(DurationUnit.MILLISECONDS)
				content = """
					${Vars.ubid} — running for ${formatTime(System.currentTimeMillis() - Vars.startedAt)}. sussification time: ${ping}ms.
					Time since flarogus epoch: ${formatTime(System.currentTimeMillis() - Vars.flarogusEpoch)}
				""".trimIndent()
			}
			
			try {
				message.delete()
			} catch (ignored: Exception) {} //lack of "manage messages" permission
		}
		.description("Show the bot status")
		
		tree("fun") {
			register(MinesweeperCommand);

			val flarsusBase = ImageIO.read({}::class.java.getResource("/flarsus.png") ?: throw RuntimeException("aaaaa le flar has escaped"))
			register("flaroficate") {
				val image = if (message.attachments.size == 0) {
					userOrAuthor(it.getOrNull(1), this)?.getAvatarUrl()
				} else {
					message.attachments.find { it.isImage && it.width!! < 2000 && it.height!! < 2000 }?.url
				}
				expect(image != null) { "failed to process: unable to retrieve image url. this can be caused by non-image files attached to the message." }
				
				val origin = ImageIO.read(URL(image))
				val sussyImage = ImageUtil.multiply(origin, flarsusBase)						
				sendImage(message, image = sussyImage)
			}
			.header("user: User? / attachment: Image")
			.description("Flaroficate the providen image, avatar of the providen user or, if neither are present, avatar of the caller")

			val vowels = listOf('a', 'A', 'e', 'E', 'i', 'I', 'o', 'O', 'u', 'U', 'y', 'Y')
			register("impostor") {
				val arg = it.getOrNull(1)
				val name = if (arg == null || arg.isEmpty() || arg[0].isDigit() || arg.startsWith("<@")) { //todo: find a better way?
					userOrAuthor(arg, this@register)?.username;
				} else {
					arg;
				}
				expect(name != null) { "the amogus has escaped, I couldn't do anything :pensive:" }
				
				message.replyWith(buildString {
					var usAdded = false
					for (i in name.length - 1 downTo 0) {
						val char = name[i]
						if (!usAdded && char.isLetter() && char !in vowels) {
							insert(0, "us")
							insert(0, char)
							usAdded = true
						} else if (usAdded || char !in vowels) {
							insert(0, char)
						}
					}
				})
			}
			.header("user: User? / single_word: String?")
			.description("Amogusificate the providen word, name of the providen user or, if neither are present, name of the caller.")
	
			register("merge") {
				val first = userOrNull(it.getOrNull(1))
				val second = userOrAuthor(it.getOrNull(2), this)
					
				expect(first != null && second != null) { "you must specify at least one valid user!" }
				expect(first != second) { "you must specify different users!" }
				
				try {
					val image1 = ImageIO.read(URL(first.getAvatarUrl()))
					val image2 = ImageIO.read(URL(second.getAvatarUrl()))
					
					val result = ImageUtil.merge(image1, image2)
					
					val name1 = first.username
					val name2 = second.username
					
					sendImage(message, name1.substring(0, name1.length / 2) + name2.substring(name2.length / 2), result)
				} catch (e: IOException) {
					throw CommandException("merge", e.stackTraceToString())
				}
			}
			.header("first: User, second: User?")
			.description("Merge pfps of two users. If only one user is specified, uses the caller as the second.")
		}
		.description("funny stuff that nobody uses")

		tree("util") {
			register(UserinfoCommand)

			register("command") {
				if (it.getOrNull(1) == null) return@register
				
				var proc: Process? = null
				val thread = Vars.threadPool.submit {
					try {
						File("/tmp/command").writeText(it.get(0))
						
						ProcessBuilder("sudo", "chmod", "+x", "/tmp/command").start().waitFor(1000, TimeUnit.MILLISECONDS)
						proc = ProcessBuilder("bash", "/tmp/command")
							.directory(File("/usr/bin"))
							.redirectOutput(ProcessBuilder.Redirect.PIPE)
							.redirectError(ProcessBuilder.Redirect.PIPE)
							.start()
						proc!!.waitFor(10000, TimeUnit.MILLISECONDS)
						proc!!.errorStream.bufferedReader().use {
							val error = it.readText()
							proc!!.inputStream.bufferedReader().use {
								message.replyWith("output${if (error != "") " and errors" else ""}:\n```\n$error\n\n${it.readText()} \n```")
							}
						}
					} catch(e: IOException) {
						message.replyWith(e.toString()) 
					}
				}
				delay(60 * 1000L) //60 seconds must be enough
				thread.cancel(true)
				if (proc != null) proc!!.destroy()
			}
			.header("bash script: String")
			.condition(CustomCommand.ownerOnly)
		}
		.description("Useless utility commands")
		
		register("shutdown") {
			val target = it.getOrNull(1)
			expect(target != null) { "no unique bot id specified" }
			
			if (target == Vars.ubid) {
				Multiverse.shutdown()
				message.replyWith("shutting down...").await()
				
				//purge
				it.getOrNull(2)?.toIntOrNull()?.let { purgeCount ->
					Multiverse.history.takeLast(min(purgeCount, 20)).forEach {
						it.retranslated.forEach { it.delete() } //origins are not deleted — use purge for that.
					}
				}
				
				Multiverse.brodcastSystem { 
					embed { description = "A Multiverse instance is shutting down..." }
				}
				
				delay(5000L)
				Vars.client.shutdown()
				//Vars.saveState()
				
				System.exit(0)
			}
		}
		.condition(CustomCommand.adminOnly)
		.header("ubid: Int, purgeCount: Int?")
		.description("shut down an instance by ubid, optionally deleting up to [purgeCount] last messages sent in this instance (use this to clear the consequences of double-instance periods)")
		
		val reportsChannel = Snowflake(944718226649124874UL)	
		register("report") {
			expect(!it[0].isEmpty()) { "you must specify a message" }
			
			try {
				Vars.client.unsafe.messageChannel(reportsChannel).createMessage {
					content = """
						${message.author?.tag} (channel ${message.channelId}, guild ${message.data.guildId.value}) reports:
						```
						${it[0].stripEveryone().take(1800)}
						```
					""".trimIndent()
				}
				
				message.replyWith("Sent succefully")
			} catch (e: Exception) {
				throw CommandException("Could not send a report", e)
			}
		}
		.header("message: String")
		.description("Send a message that will be visible to admins")
		
		register("server") {
			try {
				message.author?.getDmChannel()?.createMessage("invite to the core guild: https://discord.gg/kgGaUPx2D2")
			} catch (e: Exception) {
				replyWith(message, "couldn't send a DM. make sure you have DMs open ($e)")
			}
		}
		.description("Get an invite to the official server")
	}
}