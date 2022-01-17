package flarogus.commands

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import javax.imageio.*;
import kotlin.random.*;
import kotlin.time.*
import kotlinx.coroutines.*;
import kotlinx.coroutines.flow.*;
import dev.kord.core.event.*
import dev.kord.core.event.message.*
import dev.kord.core.supplier.*;
import dev.kord.core.entity.*;
import dev.kord.core.behavior.*
import dev.kord.core.behavior.channel.*
import dev.kord.common.entity.*
import flarogus.*
import flarogus.util.*;
import flarogus.commands.*;
import flarogus.commands.impl.*;

fun initCommands() {
	CommandHandler.register("mines", MinesweeperCommand);
	
	CommandHandler.register("userinfo", UserinfoCommand);
	
	CommandHandler.register("run", RunCommand)
	
	CommandHandler.register("help") {
		launch {
			message.channel.createEmbed {
				title = "Flarogus help"
				
				var hidden = 0
				val author = message.author
				for ((commandName, command) in CommandHandler.commands) {
					if (author == null || !command.condition(author)) {
						hidden++
						continue;
					}
					field {
						name = commandName
						value = command.description ?: "no description"
						`inline` = true
						
						if (command.header != null) name += " [" + command.header + "]"
					}
				}
				
				if (hidden > 0) {
					footer { text = "there's [$hidden] commands you are not allowed to run" }
				}
			}
		}
	}
	.setDescription("Show the help message")
	
	@OptIn(kotlin.time.ExperimentalTime::class)
	CommandHandler.register("sus") {
		sendEdited(message, "sussificating", 50L) {
			val ping = message.id.timeMark.elapsedNow().toLong(DurationUnit.MILLISECONDS)
			"${Vars.ubid} — running for ${formatTime(System.currentTimeMillis() - Vars.startedAt)}. sussification time: ${ping}ms."
		}
		message.delete()
	}
	.setDescription("Show the bot status")
	
	val flarsusBase = ImageIO.read({}::class.java.getResource("/flarsus.png") ?: throw RuntimeException("aaaaa le flar has escaped"))
	CommandHandler.register("flaroficate") {
		launch {
			val image = if (message.attachments.size == 0) {
				userOrAuthor(it.getOrNull(1), this@register)?.getAvatarUrl()
			} else { //idk it thinks .jpg is not an image format
				message.attachments.find { (it.isImage || it.filename.endsWith(".jpg")) && it.width!! < 2000 && it.height!! < 2000 }?.url
			}
			if (image == null) throw CommandException("flaroficate", "failed to process: unable to retrieve image url. this can be caused by non-image files attached to the message.")
			
			try {
				val origin = ImageIO.read(URL(image))
				val sussyImage = ImageUtil.multiply(origin, flarsusBase)
				
				sendImage(message, image = sussyImage)
			} catch (e: Exception) {
				throw CommandException("flaroficate", e.stackTraceToString())
			}
		}
	}
	.setHeader("user: User? / attachment: Image")
	.setDescription("Flaroficate the providen image, avatar of the providen user or, if neither are present, avatar of the caller")
	
	val vowels = listOf('a', 'A', 'e', 'E', 'i', 'I', 'o', 'O', 'u', 'U', 'y', 'Y')
	CommandHandler.register("impostor") {
		val arg = it.getOrNull(1)
		val name = if (arg == null || arg.isEmpty() || arg[0].isDigit() || arg.startsWith("<@")) { //todo: find a better way?
			userOrAuthor(arg, this@register)?.username;
		} else {
			arg;
		}
		if (name == null) throw CommandException("impostor", "the amogus has escaped, I couldn't do anything :pensive:")
		
		replyWith(message, buildString {
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
	.setHeader("user: User? / single_word: String?")
	.setDescription("Amogusificate the providen word, name of the providen user or, if neither are present, name of the caller.")
	
	CommandHandler.register("shutdown") {
		val target = it.getOrNull(1)
		if (target == null) throw CommandException("shutdown", "no unique bot id specified")
		
		if (target == Vars.ubid || target == "all") {
			File("done").printWriter().use { it.print(1) }
			Vars.client.shutdown()
			Vars.saveState()
			Multiverse.brodcast { content = "Multiverse is shutting down..." }
			delay(5000L)
			throw Error("shutting down...") //Error won't be caught, will crash the application and make the workflow stop
		}
	}
	.setCondition { it.id.value in Vars.runWhitelist }
	.setHeader("ubid: Int")
	.setDescription("shut down an instance by ubid.")

	flarogus.commands.CommandHandler.register("command") {
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
						replyWith(message, "output${if (error != "") " and errors" else ""}:\n```\n$error\n\n${it.readText()} \n```")
					}
				}
			} catch(e: IOException) {
				ktsinterface.launch { replyWith(message, e.toString()) }
			}
		}
		delay(60 * 1000L) //60 seconds must be enough
		thread.cancel(true)
		if (proc != null) proc!!.destroy()
	}
	.setHeader("bash script: String")
	.setCondition { it.id.value == flarogus.Vars.ownerId }
	
	CommandHandler.register("merge") {
		val first = userOrNull(it.getOrNull(1), this)
		val second = userOrAuthor(it.getOrNull(2), this)
			
		if (first == null || second == null) throw CommandException("merge", "you must specify at least one valid user! (null-equals: first - ${first == null}, second - ${second == null})")
		if (first == second) throw CommandException("merge", "you must specify different users!")
		
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
	.setHeader("first: User, second: User?")
	.setDescription("Merge pfps of two users. If only one user is specified, uses the caller as the second.")
	
	CommandHandler.register("multiverse") {
		val command = it.getOrNull(1)
		
		when (command) {
			"listGuilds" -> {
				val msg = Multiverse.multiverse.map {
					message.supplier.getGuild(it.data.guildId.value ?: return@map null)
				}.filter { it != null}.toSet().map { "${it?.id?.value} - ${it?.name?.stripEveryone()}" }.joinToString(",\n")
				replyWith(message, msg)
			}
			
			"ban" -> Multiverse.blacklist(Snowflake(it.getOrNull(1)?.toULong() ?: throw CommandException("ban", "no uid specified")))
			
			else -> replyWith(message, "unknown mutliverse command")
		}
	}
	.setHeader("command: [listGuilds, ban (id)]")
	.setDescription("Execute a multiverse command")
	.setCondition { it.id.value in Vars.runWhitelist }
	
}