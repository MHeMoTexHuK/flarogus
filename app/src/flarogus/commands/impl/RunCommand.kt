package flarogus.commands.impl

import java.io.*
import javax.script.*
import kotlinx.coroutines.*;
import flarogus.*
import flarogus.util.*

val RunCommand = flarogus.commands.Command(
	handler = {
		val command = it[0]
		val begin = command.indexOf("<<")
		
		if (begin == -1) throw CommandException("run", "Invalid syntax! The correct one is 'run -arg1 -arg2 << some_script'. The script can be wrapped in a code block.")
		
		//try to find a code block, remove it if it's present
		var script = command.substring(begin + 2)
		val codeblock = "```([a-z]*)?((?s).*)```".toRegex().find(script)?.groupValues?.getOrNull(2)
		if (codeblock != null) script = codeblock
		
		var isAdmin = false
		var stopAfter = 3000L
		
		val regex = "-([a-zA-Z0-9=]*)[\\s<]?".toRegex()
		var argument = regex.find(command.substring(0, begin - 1))
		while (argument != null) {
			val arg = argument.groupValues.getOrNull(1) ?: break
			
			when (arg) {
				"admin" -> {
					if (message.author?.id?.value != Vars.ownerId) throw IllegalAccessException("you are not allowed to use argument 'admin'!")
					isAdmin = true
				}
				"long" -> {
					if (message.author?.id?.value != Vars.ownerId) throw IllegalAccessException("you're not allowed to use argument '-long'!")
					stopAfter = 300000L
				}
				else -> throw CommandException("run", "unknown argument: $arg")
			}
			
			argument = argument.next()
		}
		
		//check for errors
		var errCount = 0
		var errors = ""
		fun illegal(cause: String, vararg illegals: String) {
			for (illegal in illegals) {
				if (script.contains(illegal)) {
					errCount++
					errors += "[ERROR]: '$illegal' $cause";
				}
			}
		}
		illegal(cause = "should not be used at all. Use `ktsinterface.launch` instead.", "runBlocking", "coroutineScope")
		if (!isAdmin) {
			illegal(
				cause = "can only be used in conjunction with argument '-admin'!\n",
				
				"Thread", "System", "java.lang.Thread", "java.lang.System",
				"Class", "KClass", "::class", ".getClass", "ClassLoader",
				"dev.kord", "KtsObjectLoader", "ScriptEngine", "flarogus.",
				"Process"
			)
		}
		if (errCount > 0) {
			throw CommandException("run", "$errCount errors:\n```\n${errors.take(1500)}\n```")
		}
		
		//execute
		if (isAdmin) {
			//application context
			val engine = ScriptEngineManager(Thread.currentThread().contextClassLoader).getEngineByExtension("kts");
			launch {
				try {
					//this script must be run in this coroutine
					ktsinterface.lastScope = this
					val result = engine.eval(script)?.toString() ?: "null"
					replyWith(message, "```\n$result\n```")
				} catch (e: Exception) { 
					val trace = if (e is ScriptException) e.toString() else e.cause?.stackTraceToString() ?: e.stackTraceToString()
					
					replyWith(message, "exception during execution:\n```\n${trace}\n```")
					e.printStackTrace()
				}
			}
		} else {
			//subprocess context
			try {
				File("/tmp/scriptfile.kts")
				
				val parts = "kotlinc -script scriptfile.kts".split("\\s".toRegex())
				val proc = ProcessBuilder(*parts.toTypedArray())
					.directory(File("/tmp"))
					.redirectOutput(ProcessBuilder.Redirect.PIPE)
					.redirectError(ProcessBuilder.Redirect.PIPE)
					.start()
				
				launch {
					delay(stopAfter)
					proc.inputStream.bufferedReader().use {
						replyWith(message, "output:\n```\n${it.readText()}\n```")
					}
					proc.destroy()
				}
			} catch(e: IOException) {
				replyWith(message, e.toString())
			}
		}
	},
	
	condition = { it.id.value == Vars.ownerId },
	
	header = "-flags] << [arbitrary kotlin script: String",
	
	description = "Execute an arbitrary kotlin script (kts) and print it's result. Unless used with '-long' or '-admin' argument, the execution time is limited to 3 seconds"
)

private class TimeoutException(message: String) : RuntimeException(message);