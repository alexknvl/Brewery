package com.dre.brewery;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

public class DrunkTextEffect {

	// represends Words and letters, that are replaced in drunk players messages

	public static ArrayList<DrunkTextEffect> words = new ArrayList<DrunkTextEffect>();
	public static List<String[]> ignoreText = new ArrayList<String[]>();

	private final String from;
	private final String to;
	private final String[] pre;
	private final Boolean match;
	private final int alcohol;
	private final int percentage;

	public DrunkTextEffect(Map<?, ?> map) {
		if (map.containsKey("replace") && map.get("replace") instanceof String)
			from = (String) map.get("replace");
		else from = null;

		if (map.containsKey("to") && map.get("to") instanceof String)
			to = (String) map.get("to");
		else to = null;

		if (map.containsKey("pre") && map.get("pre") instanceof String)
			pre = ((String) map.get("pre")).split(",");
		else pre = null;

		if (map.containsKey("match") && map.get("match") instanceof Boolean)
			match = (Boolean) map.get("match");
		else match = false;

		if (map.containsKey("alcohol") && map.get("alcohol") instanceof Integer)
			alcohol = (Integer) map.get("alcohol");
		else alcohol = 1;

		if (map.containsKey("percentage") && map.get("percentage") instanceof Integer)
			percentage = (Integer) map.get("percentage");
		else percentage = 100;
	}

	public static boolean loadWords(FileConfiguration config) {
		if (words.isEmpty()) {
			// load when first drunk player talks
			if (config != null) {
                for (Map<?, ?> map : config.getMapList("words")) {
                    DrunkTextEffect effect = new DrunkTextEffect(map);
					if (effect.from != null && effect.to != null) {
						words.add(effect);
					}
                }
            }
		}
		return !words.isEmpty();
	}

	// distorts a message, ignoring text enclosed in ignoreText letters
	public static String distortMessage(String message, int drunkeness) {
		if (!ignoreText.isEmpty()) {
			for (String[] bypass : ignoreText) {
				int indexStart = 0;
				if (!bypass[0].equals("")) {
					indexStart = message.indexOf(bypass[0]);
				}
				int indexEnd = message.length() - 1;
				if (!bypass[1].equals("")) {
					indexEnd = message.indexOf(bypass[1], indexStart + 2);
				}
				if (indexStart != -1 && indexEnd != -1) {
					if (indexEnd > indexStart + 1) {
						String ignoredMessage = message.substring(indexStart, indexEnd);
						String msg0 = message.substring(0, indexStart);
						String msg1 = message.substring(indexEnd);

						if (msg0.length() > 1) {
							msg0 = distortMessage(msg0, drunkeness);
						}
						if (msg1.length() > 1) {
							msg1 = distortMessage(msg1, drunkeness);
						}

						return msg0 + ignoredMessage + msg1;
					}
				}
			}
		}
		return distortString(message, drunkeness);
	}

	// distorts a message without checking ignoreText letters
	private static String distortString(String message, int drunkeness) {
		if (message.length() > 1) {
			for (DrunkTextEffect word : words) {
				if (word.alcohol <= drunkeness) {
					message = word.distort(message);
				}
			}
		}
		return message;
	}

	// replace "percent"% of "from" -> "to" in "words", when the string before
	// each "from" "match"es "pre"
	// Not yet ignoring case :(
	public String distort(String words) {
		String from = this.from;
		String to = this.to;

		if (from.equalsIgnoreCase("-end")) {
			from = words;
			to = words + to;
		} else if (from.equalsIgnoreCase("-start")) {
			from = words;
			to = to + words;
		} else if (from.equalsIgnoreCase("-all")) {
			from = words;
		} else if (from.equalsIgnoreCase("-space")) {
			from = " ";
		} else if (from.equalsIgnoreCase("-random")) {
			// inserts "to" on a random position in "words"
			int charIndex = (int) (Math.random() * (words.length() - 1));
			if (charIndex < words.length() / 2) {
				from = words.substring(charIndex);
				to = to + from;
			} else {
				from = words.substring(0, charIndex);
				to = from + to;
			}
		}

		if (words.contains(from)) {
			// some characters (*,?) disturb split() which then throws
			// PatternSyntaxException
			try {
				if (pre == null && percentage == 100) {
					// All occurences of "from" need to be replaced
					return words.replaceAll(from, to);
				}
				String newWords = "";
				if (words.endsWith(from)) {
					// add space to end to recognize last occurence of "from"
					words = words + " ";
				}
				// remove all "from" and split "words" there
				String[] splitted = words.split(java.util.regex.Pattern.quote(from));
				int index = 0;
				String part;

				// if there are occurences of "from"
				if (splitted.length > 1) {
					// - 1 because dont add "to" to the end of last part
					while (index < splitted.length - 1) {
						part = splitted[index];
						// add current part of "words" to the output
						newWords = newWords + part;
						// check if the part ends with correct string

						if (doesPreMatch(part) && Math.random() * 100.0 <= percentage) {
							// add replacement
							newWords = newWords + to;
						} else {
							// add original
							newWords = newWords + from;
						}
						index++;
					}
					// add the last part to finish the sentence
					part = splitted[index];
					if (part.equals(" ")) {
						// dont add the space to the end
						return newWords;
					} else {
						return newWords + part;
					}
				}
			} catch (java.util.regex.PatternSyntaxException e) {
				// e.printStackTrace();
				return words;
			}
		}
		return words;
	}

	public boolean doesPreMatch(String part) {
		boolean isBefore = !match;
		if (pre != null) {
			for (String pr : pre) {
				if (match) {
					// if one is correct, it is enough
					if (part.endsWith(pr) == match) {
						isBefore = true;
						break;
					}
				} else {
					// if one is wrong, its over
					if (part.endsWith(pr) != match) {
						isBefore = false;
						break;
					}
				}
			}
		} else {
			isBefore = true;
		}
		return isBefore;
	}

}