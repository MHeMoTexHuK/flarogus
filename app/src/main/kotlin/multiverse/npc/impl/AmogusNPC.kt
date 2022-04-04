package flarogus.multiverse.npc.impl

import kotlin.random.*
import dev.kord.core.*
import flarogus.multiverse.npc.*

class AmogusNPC : NPC(1000L * 60 * 1) {
	override val name = "local amogus"
	override val location = "oblivion settlement"
	override val avatar = "https://drive.google.com/uc?export=download&id=19jMVrZwOuWpe7vJ1Gb3Uj0tzi27kXeEY"
	
	override val dialog = buildDialog {
		- condition {
			If { it.contains("amogus") && it.length < 15 } then random {
				- "no" and random {
					- ""
					- " u"
					- " shut up"
				} and repeat(1, 5, "!")
				
				- "s" and repeat(1, 3, "us") and " " and random {
					- ""
					- "amogus"
					- "mogus"
					- "sugoma"
					- "momogus"
				}
			}
			
			If { it.contains("what") && it.contains("is") && it.contains("flarogus") } then random {
				- "flar"
				- "flarogus"
			} and " is my " and random {
				- "favorite bot"
				- "beloved"
				- "frien"
				- "friend"
			} and "!"
			
			If { isOwnMessage(lastProcessed?.referencedMessage) } then condition {
				//when someone replies something like "you're amogus"
				If { it.contains("you") && it.contains("amogus") } then random {
					- "that's a lie"
					- "lies"
					- "no"
				}
				
				//when someone asks something like "who are you"
				If { it.contains("who") && it.contains("you") } then "I'm " and random {
					- "your local amogus"
					- "just a normal fella"
					- "totally a human"
					- "a crewmate"
					- "an impostor"
					- "a fan of flarogus corporation"
				}
				
				If { it.contains("you") && it.contains("sus") } then random {
					- "shut up"
					- "no"
					- "no u"
				} and repeat(1, 5, "!")
				
				If { it.contains("shut") } then random {
					- "no"
					- "i won't"
					- "why"
					- "no u"
				}
				
				//shut up please
				If { it.contains(":sus:") } then random {
					- "I TOLD YOU TO STOP"
					- "don't you dare"
					- "staaahp"
					- "go away"
					- "touch grass"
				}
				
				//just a generic answer
				If { it.contains("you") } then random {
					- "idc"
					- "I don't care"
					- "ok"
				}
			}
			
			If { it.contains(":sus:") } then random {
				- ""
				
				- "STOP " and random {
					- "POSTING"
					- "TALKING"
					- "SPEAKING"
				} and " ABOUT " and random {
					- "AMONG US"
					- "AMONGUS"
					- "AMOGUS"
					- "SUS"
				} and "! I" and random {
					- "'M TIRED OF SEEING IT"
					- " HATE IT"
					- " SEE IT EVERYWHERE"
				} and "!!!"
				
				- "no"
				- "NO!"
				- "stop"
				
				- "shut up" and random {
					- ""
					- "!"
					- " amogus poster"
					- " crewmate"
					- " impostor"
					- " mortal"
				} and repeat(1, 2, "!")
				
				- "s" and repeat(0, 7, "us")
				- "sussy"
				- "SHUT BEFORE I BAN U"
				- run { phrasegen(6, 's', 's', 'u', 'u', 'a', 'm', 'o', 'g') } and repeat(1, 4, "!")
			}
			
			//else
			If { true } then ""
		}
	}
}

inline fun phrasegen(length: Int, vararg letters: Char) = buildString {
	repeat(length) {
		append(letters.random())
	}
}