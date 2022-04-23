package flarogus.command

import dev.kord.core.entity.*
import flarogus.*

typealias CommandAction<R> = suspend Callback<R>.() -> Unit
typealias CommandCheck = (message: Message?, args: String) -> String?

open class FlarogusCommand<R>(name: String) {
	val name = name.trim().replace(" ", "_")
	var action: CommandAction<R>? = null
	var arguments: Arguments? = null

	/** If any of the checks returns a string, it is considered that this command cannot be executed with the returned string as the cause. */
	val checks = ArrayList<CommandCheck>()

	/** The parent command tree. May be null if the command is a root command. */
	var parent: TreeCommand? = null
		set(parent: TreeCommand?) {
			var check = parent
			do {
				if (check == this) throw RuntimeException("A command tree cannot be recursive")
				check = parent!!.parent
			} while (check != null)

			field = parent
		}
	var description: String = "No description"

	/** Internal field, do not modify. */
	var requiredArguments = 0

	init {
		allCommands.add(this)
	}

	open fun action(action: CommandAction<R>) {
		this.action = action
	}

	/** Adds a check to this command */
	open fun check(check: CommandCheck) {
		checks.add(check)
	}

	/** Adds a check that doesn't allow a user to execute this command if they're not a superuser. */
	open fun adminOnly() = check { u, _ -> if (u == null || u.id in Vars.superusers) null else "This command can only be executed by admins." }

	/** Adds a check that filters bot / webhook users. */
	open fun noBots() = check { u, _ -> if (u == null || (u.author != null && !u.author!!.isBot)) null else "Bot users can't execute this command." }

	inline fun arguments(builder: Arguments.() -> Unit) {
		if (arguments == null) arguments = Arguments()
		arguments!!.apply(builder)

		//ensure they follow the "required-optional" order
		var opt = false
		arguments!!.positional.forEach {
			if (opt && it.mandatory) throw IllegalArgumentException("Mandatory arguments must come first")
			if (!it.mandatory) opt = true
		}

		updateArgumentCount()
	}

	open suspend operator fun invoke(message: Message?, argsOverride: String): Callback<R> {
		return Callback<R>(this, argsOverride, message).also { useCallback(it) }	
	}

	/** Invokes this command for a message and returns the result of this command (if there's any) */
	open suspend operator fun invoke(args: String): R? {
		return Callback<R>(this, args).also { 
			it.replyResult = false
			useCallback(it)
		}.result
	}

	/** Invokes this command for a messagee */
	open suspend operator fun invoke(message: Message): Callback<R> = invoke(message, message.content)

	/** 
	 * Executes this command with the given fallback, inflating it's arguments if they're not present.
	 * If the callback has an associated message, reports any errors by replying to it.
	 * Otherwise, rethrows them. */
	open suspend fun useCallback(callback: Callback<R>) {
		try {
			callback.command = this
			if (arguments != null && !callback.hasArgs) {
				ArgumentDecoder(arguments!!, callback).decode()
			}
			action?.invoke(callback)
		} catch (t: Throwable) {
			if (callback.originalMessage == null) throw t
			callback.reply(t)
		}
	}

	/** @return A name of this command that includes the names of all of it's parents */
	open fun getFullName(): String {
		var current: FlarogusCommand<*>? = this
		return buildString {
			do {
				append(0, " ")
				append(0, current!!.name)
				current = current!!.parent
			} while (current != null)
		}
	}

	fun updateArgumentCount() {
		requiredArguments = arguments?.positional?.fold(0) { t, arg -> if (arg.mandatory) t + 1 else t } ?: 0
	}

	companion object {
		val allCommands = HashSet<FlarogusCommand<*>>(50)
	}
}
