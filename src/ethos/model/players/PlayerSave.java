package ethos.model.players;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import java.util.Map.Entry;

import ethos.Config;
import ethos.ServerState;
import ethos.model.content.LootingBag.LootingBagItem;
import ethos.model.content.Tutorial.Stage;
import ethos.model.content.achievement_diary.DifficultyAchievementDiary.EntryDifficulty;
import ethos.model.content.achievement_diary.ardougne.ArdougneDiaryEntry;
import ethos.model.content.achievement_diary.desert.DesertDiaryEntry;
import ethos.model.content.achievement_diary.falador.FaladorDiaryEntry;
import ethos.model.content.achievement_diary.fremennik.FremennikDiaryEntry;
import ethos.model.content.achievement_diary.kandarin.KandarinDiaryEntry;
import ethos.model.content.achievement_diary.karamja.KaramjaDiaryEntry;
import ethos.model.content.achievement_diary.lumbridge_draynor.LumbridgeDraynorDiaryEntry;
import ethos.model.content.achievement_diary.morytania.MorytaniaDiaryEntry;
import ethos.model.content.achievement_diary.varrock.VarrockDiaryEntry;
import ethos.model.content.achievement_diary.western_provinces.WesternDiaryEntry;
import ethos.model.content.achievement_diary.wilderness.WildernessDiaryEntry;
import ethos.model.content.barrows.brothers.Brother;
import ethos.model.content.kill_streaks.Killstreak;
import ethos.model.content.presets.Preset;
import ethos.model.content.presets.PresetContainer;
import ethos.model.content.titles.Title;
import ethos.model.items.GameItem;
import ethos.model.items.bank.BankItem;
import ethos.model.items.bank.BankTab;
import ethos.model.players.mode.Mode;
import ethos.model.players.mode.ModeType;
import ethos.model.players.skills.farming.Farming;
import ethos.model.players.skills.slayer.SlayerMaster;
import ethos.model.players.skills.slayer.Task;
import ethos.util.Misc;
import ethos.util.log.PlayerLogging;
import ethos.util.log.PlayerLogging.LogType;
import ethos.model.content.dailytasks.DailyTasks.PossibleTasks;
import ethos.model.content.dailytasks.TaskTypes;

public class PlayerSave {

    /*public static void main(String[] args) {
        try(FileSystem zipFs = openZip(Paths.get("Data/characters.zip"))) {
            //copyCharactersToZipFs(zipFs);
        } catch(Exception e) {
            System.out.println(e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    public static FileSystem openZip(Path zipPath) throws IOException, URISyntaxException {
        Map<String, String> providerProps = new HashMap<>(); 
        providerProps.put("create", "true");

        URI zipUri = new URI("jar:file:" + zipPath.toUri().getPath());
        FileSystem zipFs = FileSystem.newFileSystem(zipUri, providerProps);
        return zipFs;
    }

    public static void copyCharactersToZipFs() {
        Path sourceFile = Paths.get("Skyn1.txt");
        Path destFile = zipFs.getPath("/Skyn1.txt");
        Files.copy( externalTxtFile, pathInZipfile, StandardCopyOption.REPLACE_EXISTING ); 
    }
    public static void writeToFileInZip(FileSystem zipFs, String file, String[] data) throws IOException {
        try(Formatter f = new Formatter(writer)) {
            f.format("%w = %d", key, value);
        Files.write(zipFs.getPath("/"+file), Arrays.asList(data), Charset.defaultCharset(), StandardOpenOption.CREATE);
        }
    
    }
    */

	/**
	 * Tells us whether or not the player exists for the specified name.
	 * 
	 * @param name
	 * @return
	 */

	public static boolean playerExists(String name) {
        File file = new File("./Data/characters/" + name + ".txt");
		return file.exists();
	}

	public static void addItemToFile(String name, int itemId, int itemAmount) {
		if (itemId < 0 || itemAmount < 0) {
			Misc.println("Illegal operation: Item id or item amount cannot be negative.");
			return;
		}
		BankItem item = new BankItem(itemId + 1, itemAmount);
		if (!playerExists(name)) {
			Misc.println("Illegal operation: Player account does not exist, validate name.");
			return;
		}
		if (PlayerHandler.isPlayerOn(name)) {
			Misc.println("Illegal operation: Attempted to modify the account of a player online.");
			return;
		}
		File character = new File("./Data/characters/" + name + ".txt");
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(character))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			reader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		BankTab[] tabs = new BankTab[] { new BankTab(0), new BankTab(1), new BankTab(2), new BankTab(3), new BankTab(4), new BankTab(5), new BankTab(6), new BankTab(7),
				new BankTab(8), };
		String token, token2;
		String[] token3 = new String[3];
		int spot = 0;
		for (String line : lines) {
			if (line == null) {
				continue;
			}
			line = line.trim();
			spot = line.indexOf("=");
			if (spot == -1) {
				continue;
			}
			token = line.substring(0, spot);
			token = token.trim();
			token2 = line.substring(spot + 1);
			token2 = token2.trim();
			token3 = token2.split("\t");
			if (token.equals("bank-tab")) {
				int tabId = Integer.parseInt(token3[0]);
				int id = Integer.parseInt(token3[1]);
				int amount = Integer.parseInt(token3[2]);
				tabs[tabId].add(new BankItem(id, amount));
			}
		}
		boolean inserted = false;
		for (BankTab tab : tabs) {
			if (tab.contains(item) && tab.spaceAvailable(item)) {
				tab.add(item);
				inserted = true;
				break;
			}
		}
		if (!inserted) {
			for (BankTab tab : tabs) {
				if (tab.freeSlots() > 0) {
					tab.add(item);
					inserted = true;
					break;
				}
			}
		}
		if (!inserted) {
			Misc.println("Item could not be added to offline account, no free space in bank.");
			return;
		}
		int startIndex = Misc.indexOfPartialString(lines, "bank-tab");
		int lastIndex = Misc.lastIndexOfPartialString(lines, "bank-tab");
		if (lastIndex != startIndex && startIndex > 0 && lastIndex > 0) {
			List<String> cutout = lines.subList(startIndex, lastIndex);
			List<String> bankData = new ArrayList<>(lastIndex - startIndex);
			for (int i = 0; i < 9; i++) {
				for (int j = 0; j < Config.BANK_SIZE; j++) {
					if (j > tabs[i].size() - 1)
						break;
					BankItem bankItem = tabs[i].getItem(j);
					if (bankItem == null) {
						continue;
					}
					bankData.add("bank-tab" + i + "\t" + bankItem.getId() + "\t" + bankItem.getAmount());
				}
			}
			lines.removeAll(cutout);
			lines.addAll(startIndex, bankData);
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(character))) {
			for (String line : lines) {
				writer.write(line);
				writer.newLine();
			}
			writer.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Tells us whether or not the specified name has the friend added.
	 * 
	 * @param name
	 * @param friend
	 * @return
	 */
	public static boolean isFriend(String name, String friend) {
		long nameLong = Misc.playerNameToInt64(friend);
		long[] friends = getFriends(name);
		if (friends != null && friends.length > 0) {
			for (int index = 0; index < friends.length; index++) {
				if (friends[index] == nameLong) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns a characters friends in the form of a long array.
	 * 
	 * @param name
	 * @return
	 */
	public static long[] getFriends(String name) {
		String line = "";
		String token = "";
		String token2 = "";
		long[] readFriends = new long[200];
		long[] friends = null;
		int totalFriends = 0;
		int index = 0;
		File input = new File("./Data/characters/" + name + ".txt");
		if (!input.exists()) {
			return null;
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(input))) {
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				int spot = line.indexOf("=");
				if (spot > -1) {
					token = line.substring(0, spot);
					token = token.trim();
					token2 = line.substring(spot + 1);
					token2 = token2.trim();
					if (token.equals("character-friend")) {
						try {
							readFriends[index] = Long.parseLong(token2);
							totalFriends++;
						} catch (NumberFormatException nfe) {
						}
					}
				}
			}
			reader.close();
			if (totalFriends > 0) {
				friends = new long[totalFriends];
				for (int i = 0; i < totalFriends; i++) {
					friends[i] = readFriends[i];
				}
				return friends;
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}

    public File playerFile(String playerName) {
        File charFile = new File("./Data/characters/" + playerName + ".txt");
        return charFile;
    }
	/**
	 * Loading
	 **/
	//@SuppressWarnings("resource")
	public static int loadGame(Player p, String playerName, String playerPass) {
		String line = "";
		String token = "";
		String token2 = "";
		String[] token3 = new String[3];
		int ReadMode = 0;
		BufferedReader characterfile = null;
		boolean File1 = false;
        // jlabranche seems like proper work to be done here
        if (playerExists(playerName)) {
            try (Reader reader = new FileReader(new File("./Data/characters/" + playerName + ".txt"))) {
                characterfile = new BufferedReader(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //Should a character be created? Potential future idea, but for now it's been decided no.
            //characterfile.createNewFile();
			Misc.println(playerName + ": character file not found.");
			p.newPlayer = false;
            return 0;
        }

        try {
            line = characterfile.readLine();
        } catch (IOException ioexception) {
            Misc.println(playerName + ": error loading file.");
            return 3;
        }
		while (line != null) {
			line = line.trim();
			int spot = line.indexOf("=");
			if (spot > -1) {
				token = line.substring(0, spot);
				token = token.trim();
				token2 = line.substring(spot + 1);
				token2 = token2.trim();
				token3 = token2.split("\t");
                //TODO: jlabranche - seems we have several amt variables perhaps this can be simplified
                int amt, amount, id, itemId, itemAmount, slot, value, stage, tabId;
				switch (token) {
                    case "character-password":
						if (playerPass.equalsIgnoreCase(token2) || Misc.basicEncrypt(playerPass).equals(token2) || Misc.md5Hash(playerPass).equals(token2)) {
							playerPass = token2;
						} else {
                            System.out.println("Password did not match character file");
							return 3;
						}
					break;
                    case "character-height":
                        p.heightLevel = Integer.parseInt(token2);
                        break;
                    case "character-hp":
                        p.getHealth().setAmount(Integer.parseInt(token2));
                        if (p.getHealth().getAmount() <= 0) {
                            p.getHealth().setAmount(10);
                        }
                        break;
                    case "character-mac-address":
						/*if (!p.getMacAddress().equalsIgnoreCase(token2)) {
							//PlayerLogging.write(LogType.CHANGE_MAC_ADDRESS, p, "Mac Address Changed: previous" + token2 + ", new" + p.getMacAddress());
						}*/
						p.setMacAddress(p.getMacAddress());
                        break;
                    case "character-ip-address":
						if (!p.getIpAddress().equalsIgnoreCase(token2)) {
							PlayerLogging.write(LogType.CHANGE_IP_ADDRESS, p, "Ip Address Changed: previous" + token2 + ", new" + p.getIpAddress());
						}
						p.setIpAddress(p.getIpAddress());
                    case "play-time":
						p.playTime = Integer.parseInt(token2);
                        break;
                    case "last-clan":
						p.setLastClanChat(token2);
                        break;
                    case "character-specRestore":
						p.specRestore = Integer.parseInt(token2);
                        break;
                    case "character-posx":
                        //TODO where is 3210? magic number.
						p.teleportToX = (Integer.parseInt(token2) <= 0 ? 3210 : Integer.parseInt(token2));
                        break;
                    case "character-posy":
                        //TODO where is 3424? magic number.
						p.teleportToY = (Integer.parseInt(token2) <= 0 ? 3424 : Integer.parseInt(token2));
                        break;
                    case "character-rights":
						p.getRights().setPrimary(Right.get(Integer.parseInt(token2)));
                        break;
                    case "character-rights-secondary": // sound like an activist group
						Arrays.stream(token3).forEach(right -> p.getRights().add(Right.get(Integer.parseInt(right))));
                        break;
                    case "revert-option":
						p.setRevertOption(token2);
                        break;
                    case "revert-delay":
						p.setRevertModeDelay(Long.parseLong(token2));
                        break;
                    case "mode":
						ModeType type = null;
						try {
							if (token2.equals("NONE")) {
								token2 = "REGULAR";
							}
							type = Enum.valueOf(ModeType.class, token2);
						} catch (NullPointerException | IllegalArgumentException e) {
							break;
						}
						Mode mode = Mode.forType(type);
						p.setMode(mode);
                        break;
                    case "tutorial-stage":
						Stage tutStage = null;
						try {
							tutStage = Enum.valueOf(Stage.class, token2);
						} catch (IllegalArgumentException | NullPointerException e) {
							break;
						}
						p.getTutorial().setStage(tutStage);
                        break;
                    case "character-title-updated":
						p.getTitles().setCurrentTitle(token2);
                        break;
                    case "experience-counter":
						p.setExperienceCounter(Long.parseLong(token2));
                        break;
                    case "killed-players":
						if (!p.getPlayerKills().getList().contains(token2))
							p.getPlayerKills().getList().add(token2);
                        break;
					case "connected-from":
						p.lastConnectedFrom.add(token2);
                        break;
					case "horror-from-deep":
						p.horrorFromDeep = Integer.parseInt(token2);
                        break;
					case "run-energy":
						p.setRunEnergy(Integer.parseInt(token2));
                        break;
					case "bank-pin":
						p.getBankPin().setPin(token2);
                        break;
					case "bank-pin-cancellation":
						p.getBankPin().setAppendingCancellation(Boolean.parseBoolean(token2));
                        break;
					case "bank-pin-cancellation-delay":
						p.getBankPin().setCancellationDelay(Long.parseLong(token2));
                        break;
					case "bank-pin-unlock-delay":
						p.getBankPin().setUnlockDelay(Long.parseLong(token2));
                        break;
					case "placeholders":
						p.placeHolders = Boolean.parseBoolean(token2);
                        break;
					case "newStarter":
						p.newStarter = Boolean.parseBoolean(token2);
                        break;
					case "dailyTaskDate":
						p.dailyTaskDate = Integer.parseInt(token2);
                        break;
					case "totalDailyDone":
						p.totalDailyDone = Integer.parseInt(token2);
                        break;
					case "currentTask":
						if(token2 != null && token2.equals("") == false)
							p.currentTask = PossibleTasks.valueOf(token2); //Integer.parseInt(token2);
                        break;
					case "completedDailyTask":
						p.completedDailyTask = Boolean.parseBoolean(token2);
                        break;
					case "playerChoice":
						if(token2 != null && token2.equals("") == false)
							p.playerChoice = TaskTypes.valueOf(token2); //Integer.parseInt(token2);
                        break;
					case "dailyEnabled":
						p.dailyEnabled = Boolean.parseBoolean(token2);
                        break;
					case "show-drop-warning":
						p.setDropWarning(Boolean.parseBoolean(token2));
                        break;
					case "hourly-box-toggle":
						p.setHourlyBoxToggle(Boolean.parseBoolean(token2));
                        break;
					case "fractured-crystal-toggle":
						p.setFracturedCrystalToggle(Boolean.parseBoolean(token2));
                        break;
					case "accept-aid":
						p.acceptAid = Boolean.parseBoolean(token2);
                        break;
					case "did-you-know":
						p.didYouKnow = Boolean.parseBoolean(token2);
                        break;
					case "raidPoints":
						p.setRaidPoints(Integer.parseInt(token2));
                        break;
					case "lootvalue":
						p.lootValue = Integer.parseInt(token2);
                        break;
					case "startPack":
						p.startPack = Boolean.parseBoolean(token2);
                        break;
					case "crystalDrop":
						p.crystalDrop = Boolean.parseBoolean(token2);
                        break;
					case "lastLoginDate":
						p.lastLoginDate = Integer.parseInt(token2);
                        break;
					case "summonId":
						p.summonId = Integer.parseInt(token2);
                        break;
					case "has-npc":
						p.hasFollower = Boolean.parseBoolean(token2);
                        break;
					case "setPin":
						p.setPin = Boolean.parseBoolean(token2);
                        break;
					case "hasBankpin":
						p.hasBankpin = Boolean.parseBoolean(token2);
                        break;
					case "rfd-gloves":
						p.rfdGloves = Integer.parseInt(token2);
                        break;
					case "wave-id":
						p.waveId = Integer.parseInt(token2);
                        break;
					case "wave-type":
						p.waveType = Integer.parseInt(token2);
                        break;
					case "wave-info":
						for (int i = 0; i < p.waveInfo.length; i++)
							p.waveInfo[i] = Integer.parseInt(token3[i]);
                        break;
					case "counters":
						for (int i = 0; i < p.counters.length; i++)
							p.counters[i] = Integer.parseInt(token3[i]);
                        break;
					case "max-cape":
						for (int i = 0; i < p.maxCape.length; i++)
							p.maxCape[i] = Boolean.parseBoolean(token3[i]);
                        break;
					case "master-clue-reqs":
						for (int i = 0; i < p.masterClueRequirement.length; i++)
							p.masterClueRequirement[i] = Integer.parseInt(token3[i]);
                        break;
					case "quickprayer":
						for (int j = 0; j < token3.length; j++) {
							p.getQuick().getNormal()[j] = Boolean.parseBoolean(token3[j]);
						}
                        break;
					case "zulrah-best-time":
						p.setBestZulrahTime(Long.parseLong(token2));
                        break;
					case "toxic-staff":
						p.setToxicStaffOfTheDeadCharge(Integer.parseInt(token2));
                        break;
					case "toxic-pipe-ammo":
						p.setToxicBlowpipeAmmo(Integer.parseInt(token2));
                        break;
					case "toxic-pipe-amount":
						p.setToxicBlowpipeAmmoAmount(Integer.parseInt(token2));
                        break;
					case "toxic-pipe-charge":
						p.setToxicBlowpipeCharge(Integer.parseInt(token2));
                        break;
					case "serpentine-helm":
						p.setSerpentineHelmCharge(Integer.parseInt(token2));
                        break;
					case "trident-of-the-seas":
						p.setTridentCharge(Integer.parseInt(token2));
                        break;
					case "trident-of-the-swamp":
						p.setToxicTridentCharge(Integer.parseInt(token2));
                        break;
					case "arclight-charge":
						p.setArcLightCharge(Integer.parseInt(token2));
                        break;
					case "crystal-bow-shots":
						p.crystalBowArrowCount = Integer.parseInt(token2);
                        break;
					case "skull-timer":
						p.skullTimer = Integer.parseInt(token2);
                        break;
					case "magic-book":
						p.playerMagicBook = Integer.parseInt(token2);
                        break;
					case "slayer-recipe":
                    case "slayer-helmet":
						p.getSlayer().setHelmetCreatable(Boolean.parseBoolean(token2));
                        break;
					case "slayer-imbued-helmet":
						p.getSlayer().setHelmetImbuedCreatable(Boolean.parseBoolean(token2));
                        break;
					case "bigger-boss-tasks":
						p.getSlayer().setBiggerBossTasks(Boolean.parseBoolean(token2));
                        break;
					case "cerberus-route":
						p.getSlayer().setCerberusRoute(Boolean.parseBoolean(token2));
                        break;
					case "superior-slayer":
						p.getSlayer().setBiggerAndBadder(Boolean.parseBoolean(token2));
                        break;
					case "slayer-tasks-completed":
						p.slayerTasksCompleted = Integer.parseInt(token2);
                        break;
					case "claimedReward":
						p.claimedReward = Boolean.parseBoolean(token2);
                        break;
					case "barrows-final-brother":
						p.getBarrows().setLastBrother(token2);
                        break;
					case "barrows-monsters-killcount":
						p.getBarrows().setMonstersKilled(Integer.parseInt(token2));
                        break;
					case "barrows-completed":
						p.getBarrows().setCompleted(Boolean.valueOf(token2));
                        break;
					case "special-amount":
						p.specAmount = Double.parseDouble(token2);
                        break;
					case "prayer-amount":
						p.prayerPoint = Double.parseDouble(token2);
                        break;
					case "dragonfire-shield-charge":
						p.setDragonfireShieldCharge(Integer.parseInt(token2));
                        break;
					case "pkp":
						p.pkp = Integer.parseInt(token2);
                        break;
					case "votePoints":
						p.votePoints = Integer.parseInt(token2);
                        break;
					case "bloodPoints":
						p.bloodPoints = Integer.parseInt(token2);
                        break;
					case "donP":
						p.donatorPoints = Integer.parseInt(token2);
                        break;
					case "donA":
						p.amDonated = Integer.parseInt(token2);
                        break;
					case "prestige-points":
						p.prestigePoints = Integer.parseInt(token2);
                        break;
                    case "xpLock":
						p.expLock = Boolean.parseBoolean(token2);
                        break;
                    case "Ahrim":
                    case "Dharok":
                    case "Guthan":
                    case "Karil":
                    case "Torag":
                    case "Verac":
                        if (p.getBarrows().getBrother(token).isPresent()) {
                            p.getBarrows().getBrother(token).get().setDefeated(Boolean.parseBoolean(token2));
                        }
                        break;
                    case "KC":
						p.killcount = Integer.parseInt(token2);
                        break;
                    case "DC":
						p.deathcount = Integer.parseInt(token2);
                        break;
                    case "last-incentive":
						p.setLastIncentive(Long.parseLong(token2));
                        break;
					case "teleblock-length":
						p.teleBlockDelay = System.currentTimeMillis();
						p.teleBlockLength = Integer.parseInt(token2);
                        break;
					case "pc-points":
						p.pcPoints = Integer.parseInt(token2);
                        break;
					case "total-rogue-kills":
						p.getBH().setTotalRogueKills(Integer.parseInt(token2));
                        break;
					case "total-hunter-kills":
						p.getBH().setTotalHunterKills(Integer.parseInt(token2));
                        break;
					case "target-time-delay":
						p.getBH().setDelayedTargetTicks(Integer.parseInt(token2));
                        break;
					case "bh-penalties":
						p.getBH().setWarnings(Integer.parseInt(token2));
                        break;
					case "bh-bounties":
						p.getBH().setBounties(Integer.parseInt(token2));
                        break;
					case "statistics-visible":
						p.getBH().setStatisticsVisible(Boolean.parseBoolean(token2));
                        break;
					case "spell-accessible":
						p.getBH().setSpellAccessible(Boolean.parseBoolean(token2));
                        break;
					case "killStreak":
						p.killStreak = Integer.parseInt(token2);
                        break;
					case "achievement-points":
						p.getAchievements().setPoints(Integer.parseInt(token2));
                        break;
					case "achievement-items":
						for (int i = 0; i < token3.length; i++)
							p.getAchievements().setBoughtItem(i, Integer.parseInt(token3[i]));
						//Varrock claimed
                        break;
					case "VarrockClaimedDiaries":
						String[] claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getVarrockDiary().claim(diff));
						}
                        break;
					case "ArdougneClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getArdougneDiary().claim(diff));
						}
                        break;
					case "DesertClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getDesertDiary().claim(diff));
						}
                        break;
					case "FaladorClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getFaladorDiary().claim(diff));
						}
                        break;
					case "FremennikClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getFremennikDiary().claim(diff));
						}
                        break;
					case "KandarinClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getKandarinDiary().claim(diff));
						}
                        break;
					case "KaramjaClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getKaramjaDiary().claim(diff));
						}
                        break;
					case "LumbridgeClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getLumbridgeDraynorDiary().claim(diff));
						}
                        break;
					case "MorytaniaClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getMorytaniaDiary().claim(diff));
						}
                        break;
					case "WesternClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getWesternDiary().claim(diff));
						}
                        break;
					case "WildernessClaimedDiaries":
						claimedRaw = token2.split(",");
						for (String claim : claimedRaw) {
							EntryDifficulty.forString(claim).ifPresent(diff -> p.getDiaryManager().getWildernessDiary().claim(diff));
						}
                        break;
					case "diaries":
						try {
                            String raw = token2;
                            String[] components = raw.split(",");
                            for (String comp : components) {
                                if (comp.isEmpty()) {
                                    continue;
                                }
                                    // Varrock
                                    Optional<VarrockDiaryEntry> varrock = VarrockDiaryEntry.fromName(comp);
                                    if (varrock.isPresent()) {
                                        p.getDiaryManager().getVarrockDiary().nonNotifyComplete(varrock.get());
                                    }
                                    // Ardougne
                                    Optional<ArdougneDiaryEntry> ardougne = ArdougneDiaryEntry.fromName(comp);
                                    if (ardougne.isPresent()) {
                                        p.getDiaryManager().getArdougneDiary().nonNotifyComplete(ardougne.get());
                                    }
                                    // Desert
                                    Optional<DesertDiaryEntry> desert = DesertDiaryEntry.fromName(comp);
                                    if (desert.isPresent()) {
                                        p.getDiaryManager().getDesertDiary().nonNotifyComplete(desert.get());
                                    }
                                    // Falador
                                    Optional<FaladorDiaryEntry> falador = FaladorDiaryEntry.fromName(comp);
                                    if (falador.isPresent()) {
                                        p.getDiaryManager().getFaladorDiary().nonNotifyComplete(falador.get());
                                    }
                                    // Fremennik
                                    Optional<FremennikDiaryEntry> fremennik = FremennikDiaryEntry.fromName(comp);
                                    if (fremennik.isPresent()) {
                                        p.getDiaryManager().getFremennikDiary().nonNotifyComplete(fremennik.get());
                                    }
                                    // Kandarin
                                    Optional<KandarinDiaryEntry> kandarin = KandarinDiaryEntry.fromName(comp);
                                    if (kandarin.isPresent()) {
                                        p.getDiaryManager().getKandarinDiary().nonNotifyComplete(kandarin.get());
                                    }
                                    // Karamja
                                    Optional<KaramjaDiaryEntry> karamja = KaramjaDiaryEntry.fromName(comp);
                                    if (karamja.isPresent()) {
                                        p.getDiaryManager().getKaramjaDiary().nonNotifyComplete(karamja.get());
                                    }
                                    // Lumbridge
                                    Optional<LumbridgeDraynorDiaryEntry> lumbridge = LumbridgeDraynorDiaryEntry.fromName(comp);
                                    if (lumbridge.isPresent()) {
                                        p.getDiaryManager().getLumbridgeDraynorDiary().nonNotifyComplete(lumbridge.get());
                                    }
                                    // Morytania
                                    Optional<MorytaniaDiaryEntry> morytania = MorytaniaDiaryEntry.fromName(comp);
                                    if (morytania.isPresent()) {
                                        p.getDiaryManager().getMorytaniaDiary().nonNotifyComplete(morytania.get());
                                    }
                                    // Western
                                    Optional<WesternDiaryEntry> western = WesternDiaryEntry.fromName(comp);
                                    if (western.isPresent()) {
                                        p.getDiaryManager().getWesternDiary().nonNotifyComplete(western.get());
                                    }
                                    // Wilderness
                                    Optional<WildernessDiaryEntry> wilderness = WildernessDiaryEntry.fromName(comp);
                                    if (wilderness.isPresent()) {
                                        p.getDiaryManager().getWildernessDiary().nonNotifyComplete(wilderness.get());
                                    }
                            }
						} catch (Exception e) {
							e.printStackTrace();
						}
                        break;
					case "partialDiaries":
						String raw = token2;
						String[] components = raw.split(",");
						try {
						for (String comp : components) {
							if (comp.isEmpty()) {
								continue;
							}
							String[] part = comp.split(":");
							stage = Integer.parseInt(part[1]);
							//Varrock
							Optional<VarrockDiaryEntry> varrock = VarrockDiaryEntry.fromName(part[0]);
							if (varrock.isPresent()) {
								p.getDiaryManager().getVarrockDiary().setAchievementStage(varrock.get(), stage, false);
							}
							//Ardougne
							Optional<ArdougneDiaryEntry> ardougne = ArdougneDiaryEntry.fromName(part[0]);
							if (ardougne.isPresent()) {
								p.getDiaryManager().getArdougneDiary().setAchievementStage(ardougne.get(), stage, false);
							}
							//Desert
							Optional<DesertDiaryEntry> desert = DesertDiaryEntry.fromName(part[0]);
							if (desert.isPresent()) {
								p.getDiaryManager().getDesertDiary().setAchievementStage(desert.get(), stage, false);
							}
							//Falador
							Optional<FaladorDiaryEntry> falador = FaladorDiaryEntry.fromName(part[0]);
							if (falador.isPresent()) {
								p.getDiaryManager().getFaladorDiary().setAchievementStage(falador.get(), stage, false);
							}
							//Fremennik
							Optional<FremennikDiaryEntry> fremennik = FremennikDiaryEntry.fromName(part[0]);
							if (fremennik.isPresent()) {
								p.getDiaryManager().getFremennikDiary().setAchievementStage(fremennik.get(), stage, false);
							}
							//Kandarin
							Optional<KandarinDiaryEntry> kandarin = KandarinDiaryEntry.fromName(part[0]);
							if (kandarin.isPresent()) {
								p.getDiaryManager().getKandarinDiary().setAchievementStage(kandarin.get(), stage, false);
							}
							//Karamja
							Optional<KaramjaDiaryEntry> karamja = KaramjaDiaryEntry.fromName(part[0]);
							if (karamja.isPresent()) {
								p.getDiaryManager().getKaramjaDiary().setAchievementStage(karamja.get(), stage, false);
							}
							//Lumbridge
							Optional<LumbridgeDraynorDiaryEntry> lumbridge = LumbridgeDraynorDiaryEntry.fromName(part[0]);
							if (lumbridge.isPresent()) {
								p.getDiaryManager().getLumbridgeDraynorDiary().setAchievementStage(lumbridge.get(), stage, false);
							}
							//Morytania
							Optional<MorytaniaDiaryEntry> morytania = MorytaniaDiaryEntry.fromName(part[0]);
							if (morytania.isPresent()) {
								p.getDiaryManager().getMorytaniaDiary().setAchievementStage(morytania.get(), stage, false);
							}
							//Western
							Optional<WesternDiaryEntry> western = WesternDiaryEntry.fromName(part[0]);
							if (western.isPresent()) {
								p.getDiaryManager().getWesternDiary().setAchievementStage(western.get(), stage, false);
							}
							//Wilderness
							Optional<WildernessDiaryEntry> wilderness = WildernessDiaryEntry.fromName(part[0]);
							if (wilderness.isPresent()) {
								p.getDiaryManager().getWildernessDiary().setAchievementStage(wilderness.get(), stage, false);
							}
						}
						} catch (Exception e) {
							e.printStackTrace();
						}
                        break;
					case "bonus-end":
						p.bonusXpTime = Long.parseLong(token2);
                        break;
					case "jail-end":
						p.jailEnd = Long.parseLong(token2);
                        break;
					case "mute-end":
						p.muteEnd = Long.parseLong(token2);
                        break;
					case "last-yell":
						p.lastYell = Long.parseLong(token2);
                        break;
					case "marketmute-end":
						p.marketMuteEnd = Long.parseLong(token2);
                        break;
					case "splitChat":
						p.splitChat = Boolean.parseBoolean(token2);
                        break;
					case "slayer-task":
						Optional<Task> task = SlayerMaster.get(token2);
						p.getSlayer().setTask(task);
                        break;
					case "slayer-master":
						p.getSlayer().setMaster(Integer.parseInt(token2));
                        break;
					case "slayerPoints":
						p.getSlayer().setPoints(Integer.parseInt(token2));
                        break;
					case "slayer-task-amount":
						p.getSlayer().setTaskAmount(Integer.parseInt(token2));
                        break;
					case "consecutive-tasks":
						p.getSlayer().setConsecutiveTasks(Integer.parseInt(token2));
                        break;
					case "mage-arena-points":
						p.setArenaPoints(Integer.parseInt(token2));
                        break;
					case "shayzien-assault-points":
						p.setShayPoints(Integer.parseInt(token2));
                        break;
					case "autoRet":
						p.autoRet = Integer.parseInt(token2);
                        break;
					case "flagged":
						p.accountFlagged = Boolean.parseBoolean(token2);
                        break;
					case "keepTitle":
						p.keepTitle = Boolean.parseBoolean(token2);
                        break;
					case "killTitle":
						p.killTitle = Boolean.parseBoolean(token2);
                        break;
					case "character-historyItems":
						for (int j = 0; j < token3.length; j++) {
							p.historyItems[j] = Integer.parseInt(token3[j]);
							p.saleItems.add(Integer.parseInt(token3[j]));
						}
                        break;
					case "character-historyItemsN":
						for (int j = 0; j < token3.length; j++) {
							p.historyItemsN[j] = Integer.parseInt(token3[j]);		
							p.saleAmount.add(Integer.parseInt(token3[j]));
						}
                        break;
					case "character-historyPrice":
						for (int j = 0; j < token3.length; j++) {
							p.historyPrice[j] = Integer.parseInt(token3[j]);		
							p.salePrice.add(Integer.parseInt(token3[j]));
						}
                        break;
					case "removed-slayer-tasks":
						if (token3.length < 4) {
							String[] backing = Misc.nullToEmpty(4);
							int index = 0;
							for (; index < token3.length; index++) {
								backing[index] = token3[index];
							}
							p.getSlayer().setRemoved(backing);
						} else if (token3.length == 4) {
							p.getSlayer().setRemoved(token3);
						}
                        break;
                    case "removedTask":
						value = Integer.parseInt(token2);
						if (value > -1) {
							p.getSlayer().setPoints(p.getSlayer().getPoints() + 100);
						}
                        break;
					case "wave":
						p.waveId = Integer.parseInt(token2);
                        break;
					case "void":
						for (int j = 0; j < token3.length; j++) {
							p.voidStatus[j] = Integer.parseInt(token3[j]);
						}
                        break;
					case "pouch-rune":
						for (int j = 0; j < token3.length; j++) {
							p.setRuneEssencePouch(j, Integer.parseInt(token3[j]));
						}
                        break;
					case "pouch-pure":
						for (int j = 0; j < token3.length; j++) {
							p.setPureEssencePouch(j, Integer.parseInt(token3[j]));
						}
                        break;
					case "gwkc":
						p.killCount = Integer.parseInt(token2);
                        break;
					case "fightMode":
						p.fightMode = Integer.parseInt(token2);
                        break;
					case "privatechat":
						p.setPrivateChat(Integer.parseInt(token2));
                        break;
					case "farming-poison-berry":
						p.getFarming().setLastBerryFarm(Long.parseLong(token2));
                    case "herb-patch 0": //Tried removing the numbers and adding a loop (didn't work?)
                        //TODO: jlabranche
                        //seems the last person who tried to convert this to a loop failed, will need to take a look.
                        p.setFarmingState(0, Integer.parseInt(token3[0]));
                        p.setFarmingSeedId(0, Integer.parseInt(token3[1]));
                        p.setFarmingTime(0, Integer.parseInt(token3[2]));
                        p.setOriginalFarmingTime(0, Integer.parseInt(token3[3]));
                        p.setFarmingHarvest(0, Integer.parseInt(token3[4]));
                        break;
					case "herb-patch 1":
                        p.setFarmingState(1, Integer.parseInt(token3[0]));
                        p.setFarmingSeedId(1, Integer.parseInt(token3[1]));
                        p.setFarmingTime(1, Integer.parseInt(token3[2]));
                        p.setOriginalFarmingTime(1, Integer.parseInt(token3[3]));
                        p.setFarmingHarvest(1, Integer.parseInt(token3[4]));
                        break;
					case "herb-patch 2":
                        p.setFarmingState(2, Integer.parseInt(token3[0]));
                        p.setFarmingSeedId(2, Integer.parseInt(token3[1]));
                        p.setFarmingTime(2, Integer.parseInt(token3[2]));
                        p.setOriginalFarmingTime(2, Integer.parseInt(token3[3]));
                        p.setFarmingHarvest(2, Integer.parseInt(token3[4]));
                        break;
					case "herb-patch 3":
							p.setFarmingState(3, Integer.parseInt(token3[0]));
							p.setFarmingSeedId(3, Integer.parseInt(token3[1]));
							p.setFarmingTime(3, Integer.parseInt(token3[2]));
							p.setOriginalFarmingTime(3, Integer.parseInt(token3[3]));
							p.setFarmingHarvest(3, Integer.parseInt(token3[4]));
                        break;
					case "herb-patch 4":
                        p.setFarmingState(4, Integer.parseInt(token3[0]));
                        p.setFarmingSeedId(4, Integer.parseInt(token3[1]));
                        p.setFarmingTime(4, Integer.parseInt(token3[2]));
                        p.setOriginalFarmingTime(4, Integer.parseInt(token3[3]));
                        p.setFarmingHarvest(4, Integer.parseInt(token3[4]));
                        break;
					case "herb-patch 5":
                        p.setFarmingState(5, Integer.parseInt(token3[0]));
                        p.setFarmingSeedId(5, Integer.parseInt(token3[1]));
                        p.setFarmingTime(5, Integer.parseInt(token3[2]));
                        p.setOriginalFarmingTime(5, Integer.parseInt(token3[3]));
                        p.setFarmingHarvest(5, Integer.parseInt(token3[4]));
                        break;
					case "herb-patch 6":
                        p.setFarmingState(6, Integer.parseInt(token3[0]));
                        p.setFarmingSeedId(6, Integer.parseInt(token3[1]));
                        p.setFarmingTime(6, Integer.parseInt(token3[2]));
                        p.setOriginalFarmingTime(6, Integer.parseInt(token3[3]));
                        p.setFarmingHarvest(6, Integer.parseInt(token3[4]));
                        break;
					case "herb-patch 7":
                        p.setFarmingState(7, Integer.parseInt(token3[0]));
                        p.setFarmingSeedId(7, Integer.parseInt(token3[1]));
                        p.setFarmingTime(7, Integer.parseInt(token3[2]));
                        p.setOriginalFarmingTime(7, Integer.parseInt(token3[3]));
                        p.setFarmingHarvest(7, Integer.parseInt(token3[4]));
                        break;
					case "compostBin":
						p.compostBin = Integer.parseInt(token2);
                        break;
                    case "halloweenOrderGiven":
						for (int i = 0; i < p.halloweenRiddleGiven.length; i++)
							p.halloweenRiddleGiven[i] = Integer.parseInt(token3[i]);
                        break;
					case "halloweenOrderChosen":
						for (int i = 0; i < p.halloweenRiddleChosen.length; i++)
							p.halloweenRiddleChosen[i] = Integer.parseInt(token3[i]);
                        break;
					case "halloweenOrderNumber":
						p.halloweenOrderNumber = Integer.parseInt(token2);
                        break;
					case "inDistrict":
						p.pkDistrict = Boolean.parseBoolean(token2);
                        break;
					case "safeBoxSlots":
						p.safeBoxSlots = Integer.parseInt(token2);
                        break;
					case "district-levels":
						for (int i = 0; i < p.playerStats.length; i++)
							p.playerStats[i] = Integer.parseInt(token3[i]);
                        break;
					case "lost-items":
						if (token3.length > 1) {
							for (int i = 0; i < token3.length; i += 2) {
								itemId = Integer.parseInt(token3[i]);
								itemAmount = Integer.parseInt(token3[i + 1]);
								p.getZulrahLostItems().add(new GameItem(itemId, itemAmount));
							}
						}
                        break;
					case "lost-items-cerberus":
						if (token3.length > 1) {
							for (int i = 0; i < token3.length; i += 2) {
								itemId = Integer.parseInt(token3[i]);
								itemAmount = Integer.parseInt(token3[i + 1]);
								p.getCerberusLostItems().add(new GameItem(itemId, itemAmount));
							}
						}
                        break;
					case "lost-items-skotizo":
						if (token3.length > 1) {
							for (int i = 0; i < token3.length; i += 2) {
								itemId = Integer.parseInt(token3[i]);
								itemAmount = Integer.parseInt(token3[i + 1]);
								p.getSkotizoLostItems().add(new GameItem(itemId, itemAmount));
							}
						}
                        break;
					case "character-equip":
						p.playerEquipment[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
						p.playerEquipmentN[Integer.parseInt(token3[0])] = Integer.parseInt(token3[2]);
                        break;
					case "character-look":
						p.playerAppearance[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
                        break;
					case "character-skill":
						p.playerLevel[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
						p.playerXP[Integer.parseInt(token3[0])] = Integer.parseInt(token3[2]);
						if (token3.length > 3) {
							p.skillLock[Integer.parseInt(token3[0])] = Boolean.parseBoolean(token3[3]);
							p.prestigeLevel[Integer.parseInt(token3[0])] = Integer.parseInt(token3[4]);
						}
                        break;
			        case "character-item":
						p.playerItems[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
						p.playerItemsN[Integer.parseInt(token3[0])] = Integer.parseInt(token3[2]);
                        break;
					case "bag-item":
						id = Integer.parseInt(token3[1]);
						amt = Integer.parseInt(token3[2]);
						p.getLootingBag().items.add(new LootingBagItem(id, amt));
                        break;
					case "item":
						itemId = Integer.parseInt(token3[0]);
						value = Integer.parseInt(token3[1]);
						String date = token3[2];
						p.getRechargeItems().loadItem(itemId, value, date);
					    break;
					case "pouch-item":
						id = Integer.parseInt(token3[1]);
						amt = Integer.parseInt(token3[2]);
						p.getRunePouch().items.add(new LootingBagItem(id, amt));
                        break;
					case "sack-item":
						id = Integer.parseInt(token3[1]);
						amt = Integer.parseInt(token3[2]);
						p.getHerbSack().items.add(new LootingBagItem(id, amt));
                        break;
                    case "safebox-item":
						id = Integer.parseInt(token3[1]);
						amt = Integer.parseInt(token3[2]);
						p.getSafeBox().items.add(new LootingBagItem(id, amt));
                        break;
					case "character-bank":
						p.bankItems[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
						p.bankItemsN[Integer.parseInt(token3[0])] = Integer.parseInt(token3[2]);
						p.getBank().getBankTab()[0].add(new BankItem(Integer.parseInt(token3[1]), Integer.parseInt(token3[2])));
                        break;
					case "bank-tab":
						tabId = Integer.parseInt(token3[0]);
						itemId = Integer.parseInt(token3[1]);
						itemAmount = Integer.parseInt(token3[2]);
						p.getBank().getBankTab()[tabId].add(new BankItem(itemId, itemAmount));
                        break;
                    case "character-friend":
                        p.getFriends().add(Long.parseLong(token3[0]));
                        break;

                    //TODO: jlabranche
                    //this case is very long src/ethos/model/content/achievement/Achievements.java the ENUM names
                    //need to modify save to save as achievement-0,1,2
                    case "achievement-0":
                        if (token3.length < 2)
                            continue;
                        p.getAchievements().read(token, 0, Integer.parseInt(token3[0]), Boolean.parseBoolean(token3[1]));
                        break;
                    case "achievement-1":
                        if (token3.length < 2)
                            continue;
                        p.getAchievements().read(token, 1, Integer.parseInt(token3[0]), Boolean.parseBoolean(token3[1]));
                        break;
                    case "achievement-2":
                        if (token3.length < 2)
                            continue;
                        p.getAchievements().read(token, 2, Integer.parseInt(token3[0]), Boolean.parseBoolean(token3[1]));
                        break;
                    case "character-ignore":
                        p.getIgnores().add(Long.parseLong(token3[0]));
                        break;

                    case "stage":
                        p.getHolidayStages().setStage(token3[0], Integer.parseInt(token3[1]));
                        break;

                    //TODO: jlabranche
                    //changed name from item to item-degrade so that there wouldn't be duplicate item case
					case "item-degrade":
						p.degradableItem[Integer.parseInt(token3[0])] = Integer.parseInt(token3[1]);
                        break;
					case "claim-state":
						for (int i = 0; i < token3.length; i++) {
							p.claimDegradableItem[i] = Boolean.parseBoolean(token3[i]);
						}
                        break;

					case "Names":
                        if (token3.length > 0) {
                            for (int i = 0; i < token3.length; i++) {
                                if (token3[i].equalsIgnoreCase("null")) {
                                    token3[i] = "New slot";
                                }
                                Preset preset = p.getPresets().getPresets().get(i);
                                preset.setAlias(token3[i]);
                            }
                        }
                        break;
                    case "Inventory":
                    case "Equipment":
                        if (token3.length > 2) {
                            int presetId = Integer.parseInt(token.split("#")[1]);
                            for (int i = 0; i < token3.length; i += 3) {
                                slot = Integer.parseInt(token3[i]);
                                itemId = Integer.parseInt(token3[i + 1]);
                                amount = Integer.parseInt(token3[i + 2]);
                                if (token.startsWith("Inventory")) {
                                    p.getPresets().getPresets().get(presetId).getInventory().getItems().put(slot, new GameItem(itemId, amount));
                                } else {
                                    p.getPresets().getPresets().get(presetId).getEquipment().getItems().put(slot, new GameItem(itemId, amount));
                                }
                            }
                        }
                        break;

                //TODO: jlabranche
                //most likely broke this
                case "killstreak":
					try {
						Killstreak.Type killstreakType = Killstreak.Type.get(token);
						value = Integer.parseInt(token2);
						p.getKillstreak().getKillstreaks().put(killstreakType, value);
					} catch (NullPointerException | NumberFormatException e) {
						e.printStackTrace();
					}
					break;

                //TODO: jlabranche
                //most likely broke this
                case "title":
					try {
						Title title = Title.valueOf(token2);
						if (title != null) {
							p.getTitles().getPurchasedList().add(title);
						}
					} catch (Exception e) {
						if (Config.SERVER_STATE == ServerState.PRIVATE) {
							e.printStackTrace();
						}
					}
					break;

                //TODO: jlabranche
                //most likely broke this
				case "npc":
					if (token != null && token.length() > 0) {
						p.getNpcDeathTracker().getTracker().put(token, Integer.parseInt(token2));
					}
					break;
				}
			}
		}
		return 13;
	}

	public static void save(Player p) {
		saveGame(p);
	}

	/**
	 * Saving
	 **/
	//@SuppressWarnings("resource")
	public static boolean saveGame(Player p) {
		if (!p.saveFile || p.newPlayer || !p.saveCharacter) {
            p.sendMessage("Failed to save: saveFile: " + p.saveFile + ", newPlayer: " + p.newPlayer + ", saveCharacter: "+ p.saveCharacter);
			return false;
		}
		if (p.playerName == null || (PlayerHandler.players[p.getIndex()] == null && p.cmdLine == 0)) {
            p.sendMessage("Failed to save: playerName: " + p.playerName +", playerIndex: " + PlayerHandler.players[p.getIndex()]);
			return false;
		}
		p.playerName = p.playerName2;
		int tbTime = (int) (p.teleBlockDelay - System.currentTimeMillis() + p.teleBlockLength);
		if (tbTime > 300000 || tbTime < 0) {
			tbTime = 0;
		}

        BufferedWriter characterfile = null;
        TreeMap<String, String> accountData = new TreeMap<>();
        accountData.put("character-username",           p.playerName);
        accountData.put("character-password",           Misc.md5Hash(p.playerPass));

        TreeMap<String, String> characterData = new TreeMap<>();
        characterData.put("character-rights",           Integer.toString(p.getRights().getPrimary().getValue()));
        characterData.put("character-mac-address",      p.getMacAddress());
        characterData.put("character-ip-address",       p.getIpAddress());
        characterData.put("revert-option",              p.getRevertOption());
//fix jlabranche
//        characterData.put("character-rights-secondary", Integer.toString(p.getRights().getSet()));
        characterData.put("character-height",           Integer.toString(p.heightLevel));
        if (p.getRevertModeDelay() > 0)
            characterData.put("revert-delay",           Long.toString(p.getRevertModeDelay()));
        if (p.getMode() != null)
            characterData.put("mode",                   p.getMode().getType().name());
        if (p.getTutorial().getStage() != null)
            characterData.put("tutorial-stage",         p.getTutorial().getStage().name());
        characterData.put("character-hp",               Integer.toString(p.getHealth().getAmount()));
        characterData.put("play-time",                  Integer.toString(p.playTime));
        characterData.put("last-clan",                  p.getLastClanChat());
        characterData.put("character-specRestore",      Integer.toString(p.specRestore));
        characterData.put("character-posx",             Integer.toString(p.absX));
        characterData.put("character-posy",             Integer.toString(p.absY));
        characterData.put("bank-pin",                   p.getBankPin().getPin());
        characterData.put("bank-pin-cancellation",      Boolean.toString(p.getBankPin().isAppendingCancellation()));
        characterData.put("bank-pin-unlock-delay",      Long.toString(p.getBankPin().getUnlockDelay()));
        characterData.put("placeholders",               Boolean.toString(p.placeHolders));
        characterData.put("newStarter",                 Boolean.toString(p.newStarter));
        characterData.put("bankpin-cancellation-delay", Long.toString(p.getBankPin().getCancellationDelay()));
        characterData.put("dailyTaskDate",              Integer.toString(p.dailyTaskDate));
        characterData.put("totalDailyDone",             Integer.toString(p.totalDailyDone));
// fix jlabranche
//        characterData.put("currentTask",                p.currentTask.name());
        characterData.put("completedDailyTask",         Boolean.toString(p.completedDailyTask));
// fix jlabranche
//        characterData.put("playerChoice",               p.playerChoice.name());
        characterData.put("dailyEnabled",               Boolean.toString(p.dailyEnabled));
        characterData.put("show-drop-warning",          Boolean.toString(p.showDropWarning()));
        characterData.put("hourly-box-toggle",          Boolean.toString(p.getHourlyBoxToggle()));
        characterData.put("fractured-crystal-toggle",   Boolean.toString(p.getFracturedCrystalToggle()));
        characterData.put("accept-aid",                 Boolean.toString(p.acceptAid));
        characterData.put("did-you-know",               Boolean.toString(p.didYouKnow));
        characterData.put("lootvalue",                  Integer.toString(p.lootValue));
        characterData.put("raidPoints",                 Integer.toString(p.getRaidPoints()));
        characterData.put("experience-counter",         Long.toString(p.getExperienceCounter()));
        characterData.put("character-title-updated",    p.getTitles().getCurrentTitle());
        for (int i = 0; i < p.lastConnectedFrom.size(); i++) {
            characterData.put("connected-from",         p.lastConnectedFrom.get(i));
        }
        for (int i = 0; i < p.getPlayerKills().getList().size(); i++) {
            characterData.put("killed-players",         p.getPlayerKills().getList().get(i));
        }
        characterData.put("lastLoginDate",              Integer.toString(p.lastLoginDate));
        characterData.put("has-npc",                    Boolean.toString(p.hasFollower));
        characterData.put("summonId",                   Integer.toString(p.summonId));
        characterData.put("startPack",                  Boolean.toString(p.startPack));
        characterData.put("crystalDrop",                Boolean.toString(p.crystalDrop));
        characterData.put("setPin",                     Boolean.toString(p.setPin));
        characterData.put("slayer-helmet",              Boolean.toString(p.getSlayer().isHelmetCreatable()));
        characterData.put("slayer-imbued-helmet",       Boolean.toString(p.getSlayer().isHelmetImbuedCreatable()));
        characterData.put("bigger-boss-tasks",          Boolean.toString(p.getSlayer().isBiggerBossTasks()));
        characterData.put("cerberus-route",             Boolean.toString(p.getSlayer().isCerberusRoute()));
        characterData.put("superior-slayer",            Boolean.toString(p.getSlayer().isBiggerAndBadder()));
        characterData.put("slayer-tasks-completed",     Integer.toString(p.slayerTasksCompleted));
        characterData.put("claimedReward",              Boolean.toString(p.claimedReward));
        characterData.put("dragonfire-shield-charge",   Integer.toString(p.getDragonfireShieldCharge()));
        characterData.put("rfd-gloves",                 Integer.toString(p.rfdGloves));
        characterData.put("wave-id",                    Integer.toString(p.waveId));
        characterData.put("wave-type",                  Integer.toString(p.waveType));
        characterData.put("wave-info",                  p.waveInfo[0] + "\t" + p.waveInfo[1] + "\t" + p.waveInfo[2]);
        characterData.put("master-clue-reqs",           p.masterClueRequirement[0] + "\t" + p.masterClueRequirement[1] + "\t" + p.masterClueRequirement[2] + "\t" + p.masterClueRequirement[3]);
//        characterData.put("counters", p.counters[i] + ((i == p.counters.length - 1) ? "" : "\t"));
/*
        for (int i = 0; i < p.counters.length; i++)
            characterData.put("counters-"+i, p.counters[i]);
        for (int i = 0; i < p.maxCape.length; i++)
            characterData.put("max-cape-"+i, p.maxCape[i]);
        }
*/
        characterData.put("zulrah-best-time",           Long.toString(p.getBestZulrahTime()));
        characterData.put("toxic-staff",                Integer.toString(p.getToxicStaffOfTheDeadCharge()));
        characterData.put("toxic-pipe-ammo",            Integer.toString(p.getToxicBlowpipeAmmo()));
        characterData.put("toxic-pipe-amount",          Integer.toString(p.getToxicBlowpipeAmmoAmount()));
        characterData.put("toxic-pipe-charge",          Integer.toString(p.getToxicBlowpipeCharge()));
        characterData.put("serpentine-helm",            Integer.toString(p.getSerpentineHelmCharge()));
        characterData.put("trident-of-the-seas",        Integer.toString(p.getTridentCharge()));
        characterData.put("trident-of-the-swamp",       Integer.toString(p.getToxicTridentCharge()));
        characterData.put("arclight-charge",            Integer.toString(p.getArcLightCharge()));
        characterData.put("slayerPoints",               Integer.toString(p.getSlayer().getPoints()));
        characterData.put("crystal-bow-shots",          Integer.toString(p.crystalBowArrowCount));
        characterData.put("skull-timer",                Integer.toString(p.skullTimer));
        characterData.put("magic-book",                 Integer.toString(p.playerMagicBook));
        for (Brother brother : p.getBarrows().getBrothers()) {
            characterData.put(brother.getName().toLowerCase(), Boolean.toString(brother.isDefeated()));
        }
        if (p.getBarrows().getLastBrother().isPresent()) {
            characterData.put("barrows-final-brother",  p.getBarrows().getLastBrother().get().getName());
        }
        characterData.put("barrows-monsters-killcount", Integer.toString(p.getBarrows().getMonsterKillCount()));
        characterData.put("barrows-completed",          Boolean.toString(p.getBarrows().isCompleted()));
        characterData.put("special-amount",             Double.toString(p.specAmount));
        characterData.put("prayer-amount",              Double.toString(p.prayerPoint));
        characterData.put("KC",                         Integer.toString(p.killcount));
        characterData.put("DC",                         Integer.toString(p.deathcount));
        characterData.put("total-hunter-kills",         Integer.toString(p.getBH().getTotalHunterKills()));
        characterData.put("total-rogue-kills",          Integer.toString(p.getBH().getTotalRogueKills()));
        characterData.put("target-time-delay",          Integer.toString(p.getBH().getDelayedTargetTicks()));
        characterData.put("bh-penalties",               Integer.toString(p.getBH().getWarnings()));
        characterData.put("bh-bounties",                Integer.toString(p.getBH().getBounties()));
        characterData.put("statistics-visible",         Boolean.toString(p.getBH().isStatisticsVisible()));
        characterData.put("spell-accessible",           Boolean.toString(p.getBH().isSpellAccessible()));
//        characterData.put("zerkAmount", 0, 13);
        characterData.put("pkp",                        Integer.toString(p.pkp));
        characterData.put("donP",                       Integer.toString(p.donatorPoints));
        characterData.put("donA",                       Integer.toString(p.amDonated));
        characterData.put("prestige-points",            Integer.toString(p.prestigePoints));
        characterData.put("votePoints",                 Integer.toString(p.votePoints));
        characterData.put("bloodPoints",                Integer.toString(p.bloodPoints));
        characterData.put("achievement-points",         Integer.toString(p.getAchievements().getPoints()));

        for (int i = 0; i < p.getAchievements().getBoughtItems().length; i++)
            characterData.put("achievement-items-"+i,   Integer.toString(p.getAchievements().getBoughtItems()[i][1]));

        characterData.put("xpLock",                     Boolean.toString(p.expLock));
        characterData.put("teleblock-length",           Integer.toString(tbTime));
        characterData.put("last-incentive",             Long.toString(p.getLastIncentive()));
        characterData.put("rfd-round",                  Integer.toString(p.rfdRound));
        characterData.put("run-energy",                 Integer.toString(p.getRunEnergy()));
        characterData.put("pc-points",                  Integer.toString(p.pcPoints));
        characterData.put("killStreak",                 Integer.toString(p.killStreak));
        characterData.put("bonus-end",                  Long.toString(p.bonusXpTime));
        characterData.put("jail-end",                   Long.toString(p.jailEnd));
        characterData.put("mute-end",                   Long.toString(p.muteEnd));
        characterData.put("marketmute-end",             Long.toString(p.marketMuteEnd));
        characterData.put("last-yell",                  Long.toString(p.lastYell));
        characterData.put("splitChat",                  Boolean.toString(p.splitChat));
        if (p.getSlayer().getTask().isPresent()) {
            Task task = p.getSlayer().getTask().get();
            characterData.put("slayer-task",            task.getPrimaryName());
            characterData.put("slayer-task-amount",     Integer.toString(p.getSlayer().getTaskAmount()));
        }
        characterData.put("slayer-master",              Integer.toString(p.getSlayer().getMaster()));
        characterData.put("consecutive-tasks",          Integer.toString(p.getSlayer().getConsecutiveTasks()));
        characterData.put("mage-arena-points",          Integer.toString(p.getArenaPoints()));
        characterData.put("shayzien-assault-points",    Integer.toString(p.getShayPoints()));
        characterData.put("autoRet",                    Integer.toString(p.autoRet));
        characterData.put("flagged",                    Boolean.toString(p.accountFlagged));
        characterData.put("keepTitle",                  Boolean.toString(p.keepTitle));
        characterData.put("killTitle",                  Boolean.toString(p.killTitle));
        characterData.put("wave",                       Integer.toString(p.waveId));
        characterData.put("gwkc",                       Integer.toString(p.killCount));
        characterData.put("fightMode",                  Integer.toString(p.fightMode));
        characterData.put("privatechat",                Integer.toString(p.getPrivateChat()));
        characterData.put("pouch-rune",                 p.getRuneEssencePouch(0) + "\t" + p.getRuneEssencePouch(1) + "\t" + p.getRuneEssencePouch(2));
        characterData.put("pouch-pure",                 p.getPureEssencePouch(0) + "\t" + p.getPureEssencePouch(1) + "\t" + p.getPureEssencePouch(2));
        characterData.put("crabsKilled",                Integer.toString(p.crabsKilled));
        characterData.put("farming-poison-berry",       Long.toString(p.getFarming().getLastBerryFarm()));

//        writeToFileInZip(zipFs, p.playerName + ".txt", characterData);
		try {
            characterfile = new BufferedWriter(new FileWriter("./Data/characters/" + p.playerName + ".txt"));
            p.sendMessage("./Data/characters/" + p.playerName + ".txt");
            TreeMap[] dataMaps   = {accountData, characterData};
            String[] map_strings = {"ACCOUNT",   "CHARACTER"};
            for (int i = 0; i < dataMaps.length; i++) {
                characterfile.write("["+map_strings[i]+"]", 0, map_strings[i].length() + 2);
                characterfile.newLine();
                //java think this is unsafe, maybe investigate a better solution in the future
                Set<Entry<String, String>> entries = dataMaps[i].entrySet();
                for (Entry<String, String> entry : entries) {
                    if (entry.getKey() != null) {
                        if (entry.getValue() != null) {
                            characterfile.write(entry.getKey() + " = ", 0, entry.getKey().length() + 3);
                            characterfile.write(entry.getValue(), 0, entry.getValue().length());
                            characterfile.newLine();
                        } else {
                            p.sendMessage("k: " + entry.getKey() + " v: " + entry.getValue());
                        }
                    }
                }
                characterfile.newLine();
            }
			characterfile.close();

			/* ACCOUNT */

			/* CHARACTER */
            /* add this back in jlabranche
			String[] removed = p.getSlayer().getRemoved();
			characterfile.write("removed-slayer-tasks");
			for (int index = 0; index < removed.length; index++) {
				characterfile.write(removed[index]);
				if (index < removed.length - 1) {
					characterfile.write("\t");
				}
			}
			
			for(int i = 0; i < p.historyItems.length; i++) {
				if(p.saleItems.size() > 0)
					p.historyItems[i] = p.saleItems.get(i).intValue();
			}
			characterfile.write("character-historyItems", 0, 25);
			String toWrite = "";
			for(int i1 = 0; i1 < p.historyItems.length; i1++) {
				toWrite += p.historyItems[i1] +"\t";
			}
			characterfile.write(toWrite);
			for(int i = 0; i < p.historyItemsN.length; i++) {
				if(p.saleItems.size() > 0)
					p.historyItemsN[i] = p.saleAmount.get(i).intValue();
			}
			characterfile.write("character-historyItemsN", 0, 26);
			String toWrite2 = "";
			for(int i1 = 0; i1 < p.historyItemsN.length; i1++) {
				toWrite2 += p.historyItemsN[i1] +"\t";
			}
			characterfile.write(toWrite2);

			for(int i = 0; i < p.historyPrice.length; i++) {
				if(p.salePrice.size() > 0)
					p.historyPrice[i] = p.salePrice.get(i).intValue();
			}
			characterfile.write("character-historyPrice", 0, 25);
			String toWrite3 = "";
			for(int i1 = 0; i1 < p.historyPrice.length; i1++) {
				toWrite3 += p.historyPrice[i1] +"\t";
			}
			characterfile.write(toWrite3);
				
			//Varrock
	        String varrockClaimed = "VarrockClaimedDiaries";
	        characterfile.write(varrockClaimed, 0, varrockClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getVarrockDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Ardougne
	        String ardougneClaimed = "ArdougneClaimedDiaries";
	        characterfile.write(ardougneClaimed, 0, ardougneClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getArdougneDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Desert
	        String desertClaimed = "DesertClaimedDiaries";
	        characterfile.write(desertClaimed, 0, desertClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getDesertDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Falador
	        String faladorClaimed = "FaladorClaimedDiaries";
	        characterfile.write(faladorClaimed, 0, faladorClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getFaladorDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Fremennik
	        String fremennikClaimed = "FremennikClaimedDiaries";
	        characterfile.write(fremennikClaimed, 0, fremennikClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getFremennikDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Kandarin
	        String kandarinClaimed = "KandarinClaimedDiaries";
	        characterfile.write(kandarinClaimed, 0, kandarinClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getKandarinDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Karamja
	        String karamjaClaimed = "KaramjaClaimedDiaries";
	        characterfile.write(karamjaClaimed, 0, karamjaClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getKaramjaDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Lumbridge
	        String lumbridgeClaimed = "LumbridgeClaimedDiaries";
	        characterfile.write(lumbridgeClaimed, 0, lumbridgeClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getLumbridgeDraynorDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Morytania
	        String morytaniaClaimed = "MorytaniaClaimedDiaries";
	        characterfile.write(morytaniaClaimed, 0, morytaniaClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getMorytaniaDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Western
	        String westernClaimed = "WesternClaimedDiaries";
	        characterfile.write(westernClaimed, 0, westernClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getWesternDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
	        //Wilderness
	        String wildernessClaimed = "WildernessClaimedDiaries";
	        characterfile.write(wildernessClaimed, 0, wildernessClaimed.length());
	        {
	            String prefix = "";
	        StringBuilder bldr = new StringBuilder();
	            for (EntryDifficulty entry : p.getDiaryManager().getWildernessDiary().getClaimed()) {
	                bldr.append(prefix);
	                prefix = ",";
	                bldr.append(entry.name());
	            }
	        characterfile.write(bldr.toString(), 0, bldr.toString().length());
	        }
			
			String diary = "diaries";
			characterfile.write(diary, 0, diary.length());
			{
				String prefix = "";
                StringBuilder bldr = new StringBuilder();
			
				// Varrock
				for (VarrockDiaryEntry entry : p.getDiaryManager().getVarrockDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Ardougne
				for (ArdougneDiaryEntry entry : p.getDiaryManager().getArdougneDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Desert
				for (DesertDiaryEntry entry : p.getDiaryManager().getDesertDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Falador
				for (FaladorDiaryEntry entry : p.getDiaryManager().getFaladorDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Fremennik
				for (FremennikDiaryEntry entry : p.getDiaryManager().getFremennikDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Kandarin
				for (KandarinDiaryEntry entry : p.getDiaryManager().getKandarinDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Karamja
				for (KaramjaDiaryEntry entry : p.getDiaryManager().getKaramjaDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Lumbridge
				for (LumbridgeDraynorDiaryEntry entry : p.getDiaryManager().getLumbridgeDraynorDiary()
						.getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Morytania
				for (MorytaniaDiaryEntry entry : p.getDiaryManager().getMorytaniaDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Western
				for (WesternDiaryEntry entry : p.getDiaryManager().getWesternDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
				// Wilderness
				for (WildernessDiaryEntry entry : p.getDiaryManager().getWildernessDiary().getAchievements()) {
					bldr.append(prefix);
					prefix = ",";
					bldr.append(entry.name());
				}
			characterfile.write(bldr.toString(), 0, bldr.toString().length());
			}
			
			String partialDiary = "partialDiaries";
			//forEachPartial
			characterfile.write(partialDiary, 0, partialDiary.length()); //Saw that earlier but forgot lol, ahh ty
			{
				StringBuilder bldr = new StringBuilder();
				String prefix = "";
				//Varrock
				for (Entry<VarrockDiaryEntry, Integer> keyval : p.getDiaryManager().getVarrockDiary().getPartialAchievements().entrySet()) {
					VarrockDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Ardougne
				for (Entry<ArdougneDiaryEntry, Integer> keyval : p.getDiaryManager().getArdougneDiary().getPartialAchievements().entrySet()) {
					ArdougneDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Desert
				for (Entry<DesertDiaryEntry, Integer> keyval : p.getDiaryManager().getDesertDiary().getPartialAchievements().entrySet()) {
					DesertDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Falador
				for (Entry<FaladorDiaryEntry, Integer> keyval : p.getDiaryManager().getFaladorDiary().getPartialAchievements().entrySet()) {
					FaladorDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Fremennik
				for (Entry<FremennikDiaryEntry, Integer> keyval : p.getDiaryManager().getFremennikDiary().getPartialAchievements().entrySet()) {
					FremennikDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Kandarin
				for (Entry<KandarinDiaryEntry, Integer> keyval : p.getDiaryManager().getKandarinDiary().getPartialAchievements().entrySet()) {
					KandarinDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Karamja
				for (Entry<KaramjaDiaryEntry, Integer> keyval : p.getDiaryManager().getKaramjaDiary().getPartialAchievements().entrySet()) {
					KaramjaDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Lumbridge
				for (Entry<LumbridgeDraynorDiaryEntry, Integer> keyval : p.getDiaryManager().getLumbridgeDraynorDiary().getPartialAchievements().entrySet()) {
					LumbridgeDraynorDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Morytania
				for (Entry<MorytaniaDiaryEntry, Integer> keyval : p.getDiaryManager().getMorytaniaDiary().getPartialAchievements().entrySet()) {
					MorytaniaDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Western
				for (Entry<WesternDiaryEntry, Integer> keyval : p.getDiaryManager().getWesternDiary().getPartialAchievements().entrySet()) {
					WesternDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				//Wilderness
				for (Entry<WildernessDiaryEntry, Integer> keyval : p.getDiaryManager().getWildernessDiary().getPartialAchievements().entrySet()) {
					WildernessDiaryEntry entry = keyval.getKey();
					int stage = keyval.getValue();
					bldr.append(prefix);
					prefix = ",";
					 bldr.append(entry.name() + ":" + stage);
				}
				characterfile.write(bldr.toString(), 0, bldr.toString().length());
			}
            */
			
			
/*			characterData.put("void",                       );
			String toWrite55 = p.voidStatus[0] + "\t" + p.voidStatus[1] + "\t" + p.voidStatus[2] + "\t" + p.voidStatus[3] + "\t" + p.voidStatus[4];
			characterfile.write(toWrite55);
			characterfile.write("quickprayer", 0, 14);
			String quick = "";
			for(int i = 0; i < p.getQuick().getNormal().length; i++) {
				quick += p.getQuick().getNormal()[i]+"\t";
			}
			characterfile.write(quick);
*/
/* readd this jlabranche
            for (int i = 0; i < Farming.MAX_PATCHES; i++) {
                characterfile.write("herb-patch "+i+" = "+p.getFarmingState(i)+"\t"+p.getFarmingSeedId(i)+"\t"+p.getFarmingTime(i)+"\t"+p.getOriginalFarmingTime(i)+"\t"+p.getFarmingHarvest(i));
            }
			characterfile.write("compostBin" + p.compostBin);
*/
/* readd this jlabranche
			characterfile.write("halloweenOrderGiven");
			for (int i = 0; i < p.halloweenRiddleGiven.length; i++)
				characterfile.write("" + p.halloweenRiddleGiven[i] + ((i == p.halloweenRiddleGiven.length - 1) ? "" : "\t"));
*/

/* readd this jlabranche
			characterfile.write("halloweenOrderChosen");
			for (int i = 0; i < p.halloweenRiddleChosen.length; i++)
				characterfile.write("" + p.halloweenRiddleChosen[i] + ((i == p.halloweenRiddleChosen.length - 1) ? "" : "\t"));
*/

			characterData.put("halloweenOrderNumber", Integer.toString(p.halloweenOrderNumber));
/* readd this jlabranche
			characterfile.write("district-levels");
			for (int i = 0; i < p.playerStats.length; i++)
				characterfile.write("" + p.playerStats[i] + ((i == p.playerStats.length - 1) ? "" : "\t"));
*/
			characterData.put("inDistrict", Boolean.toString(p.pkDistrict));
            characterData.put("safeBoxSlots", Integer.toString(p.safeBoxSlots));
/* readd this jlabranche
			characterfile.write("lost-items");
			for (GameItem item : p.getZulrahLostItems()) {
				if (item == null) {
					continue;
				}
				characterfile.write(item.getId() + "\t" + item.getAmount() + "\t");
			}
			characterfile.write("lost-items-cerberus");
			for (GameItem item : p.getCerberusLostItems()) {
				if (item == null) {
					continue;
				}
				characterfile.write(item.getId() + "\t" + item.getAmount() + "\t");
			}
			characterfile.write("lost-items-skotizo");
			for (GameItem item : p.getSkotizoLostItems()) {
				if (item == null) {
					continue;
				}
				characterfile.write(item.getId() + "\t" + item.getAmount() + "\t");
			}
*/
/* readd this jlabranche
			characterfile.write("[EQUIPMENT]", 0, 11);
			for (int i = 0; i < p.playerEquipment.length; i++) {
				characterfile.write("character-equip", 0, 18);
				characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerEquipment[i]), 0, Integer.toString(p.playerEquipment[i]).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerEquipmentN[i]), 0, Integer.toString(p.playerEquipmentN[i]).length());
				characterfile.write("	", 0, 1);
			}

			characterfile.write("[LOOK]", 0, 6);
			for (int i = 0; i < p.playerAppearance.length; i++) {
				characterfile.write("character-look", 0, 17);
				characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerAppearance[i]), 0, Integer.toString(p.playerAppearance[i]).length());
			}

			characterfile.write("[SKILLS]", 0, 8);
			for (int i = 0; i < p.playerLevel.length; i++) {
				characterfile.write("character-skill", 0, 18);
				characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerLevel[i]), 0, Integer.toString(p.playerLevel[i]).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.playerXP[i]), 0, Integer.toString(p.playerXP[i]).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Boolean.toString(p.skillLock[i]), 0, Boolean.toString(p.skillLock[i]).length());
				characterfile.write("	", 0, 1);
				characterfile.write(Integer.toString(p.prestigeLevel[i]), 0, Integer.toString(p.prestigeLevel[i]).length());
			}

			characterfile.write("[ITEMS]", 0, 7);
			for (int i = 0; i < p.playerItems.length; i++) {
				if (p.playerItems[i] > 0) {
					characterfile.write("character-item", 0, 17);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(p.playerItems[i]), 0, Integer.toString(p.playerItems[i]).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(p.playerItemsN[i]), 0, Integer.toString(p.playerItemsN[i]).length());
				}
			}
			
			characterfile.write("[RECHARGEITEMS]", 0, 15);
			for (int itemId : p.getRechargeItems().getItemValues().keySet()) {
				int value = p.getRechargeItems().getChargesLeft(itemId);
				
				String itemIdString = Integer.toString(itemId);
				String valueString = Integer.toString(value);
				String lastUsed = p.getRechargeItems().getItemLastUsed(itemId);
				
				characterfile.write("item", 0, 7);
				characterfile.write("	", 0, 1);
				characterfile.write(itemIdString, 0, itemIdString.length());
				characterfile.write("	", 0, 1);
				characterfile.write(valueString, 0, valueString.length());
				characterfile.write("	", 0, 1);
				characterfile.write(lastUsed, 0, lastUsed.length());
			}

			characterfile.write("[BANK]", 0, 6);
			for (int i = 0; i < 9; i++) {
				for (int j = 0; j < Config.BANK_SIZE; j++) {
					if (j > p.getBank().getBankTab()[i].size() - 1)
						break;
					BankItem item = p.getBank().getBankTab()[i].getItem(j);
					if (item == null) {
						continue;
					}
					
					characterfile.write("bank-tab" + i + "\t" + item.getId() + "\t" + item.getAmount());
				}
			}
			
			characterfile.write("[LOOTBAG]", 0, 9);
			for (int i = 0; i < p.getLootingBag().items.size(); i++) {
				if (p.getLootingBag().items.get(i).getId() > 0) {
					characterfile.write("bag-item", 0, 11);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					int id = p.getLootingBag().items.get(i).getId();
					int amt = p.getLootingBag().items.get(i).getAmount();
					characterfile.write(Integer.toString(id), 0, Integer.toString(id).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(amt), 0, Integer.toString(amt).length());
				}
			}

			characterfile.write("[RUNEPOUCH]", 0, 11);
			for (int i = 0; i < p.getRunePouch().items.size(); i++) {
				if (p.getRunePouch().items.get(i).getId() > 0) {
					characterfile.write("pouch-item", 0, 13);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					int id = p.getRunePouch().items.get(i).getId();
					int amt = p.getRunePouch().items.get(i).getAmount();
					characterfile.write(Integer.toString(id), 0, Integer.toString(id).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(amt), 0, Integer.toString(amt).length());
				}
			}
			
			characterfile.write("[HERBSACK]", 0, 10);
			for (int i = 0; i < p.getHerbSack().items.size(); i++) {
				if (p.getHerbSack().items.get(i).getId() > 0) {
					characterfile.write("sack-item", 0, 12);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					int id = p.getHerbSack().items.get(i).getId();
					int amt = p.getHerbSack().items.get(i).getAmount();
					characterfile.write(Integer.toString(id), 0, Integer.toString(id).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(amt), 0, Integer.toString(amt).length());
				}
			}
			
			characterfile.write("[GEMBAG]", 0, 8);
			for (int i = 0; i < p.getGemBag().items.size(); i++) {
				if (p.getGemBag().items.get(i).getId() > 0) {
					characterfile.write("bag-item", 0, 11);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					int id = p.getGemBag().items.get(i).getId();
					int amt = p.getGemBag().items.get(i).getAmount();
					characterfile.write(Integer.toString(id), 0, Integer.toString(id).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(amt), 0, Integer.toString(amt).length());
				}
			}
			
			characterfile.write("[SAFEBOX]", 0, 9);
			for (int i = 0; i < p.getSafeBox().items.size(); i++) {
				if (p.getSafeBox().items.get(i).getId() > 0) {
					characterfile.write("safebox-item", 0, 15);
					characterfile.write(Integer.toString(i), 0, Integer.toString(i).length());
					characterfile.write("	", 0, 1);
					int id = p.getSafeBox().items.get(i).getId();
					int amt = p.getSafeBox().items.get(i).getAmount();
					characterfile.write(Integer.toString(id), 0, Integer.toString(id).length());
					characterfile.write("	", 0, 1);
					characterfile.write(Integer.toString(amt), 0, Integer.toString(amt).length());
				}
			}
			
			characterfile.write("[FRIENDS]", 0, 9);
			for (Long friend : p.getFriends().getFriends()) {
				characterfile.write("character-friend", 0, 19);
				characterfile.write(Long.toString(friend), 0, Long.toString(friend).length());
			}

			characterfile.write("[HOLIDAY-EVENTS]");
			for (Entry<String, Integer> entry : p.getHolidayStages().getStages().entrySet()) {
				String key = entry.getKey();
				int value = entry.getValue();
				if (Objects.isNull(key)) {
					continue;
				}
				characterfile.write("stage" + key + "\t" + value);
			}

			characterfile.write("[DEGRADEABLES]");
			characterfile.write("claim-state");
			for (int i = 0; i < p.claimDegradableItem.length; i++) {
				characterfile.write(Boolean.toString(p.claimDegradableItem[i]));
				if (i != p.claimDegradableItem.length - 1) {
					characterfile.write("\t");
				}
			}
			for (int i = 0; i < p.degradableItem.length; i++) {
				if (p.degradableItem[i] > 0) {
					characterfile.write("item" + i + "\t" + p.degradableItem[i]);
				}
			}

			characterfile.write("[ACHIEVEMENTS-TIER-1]");
			p.getAchievements().print(characterfile, 0);

			characterfile.write("[ACHIEVEMENTS-TIER-2]");
			p.getAchievements().print(characterfile, 1);

			characterfile.write("[ACHIEVEMENTS-TIER-3]");
			p.getAchievements().print(characterfile, 2);

			characterfile.write("[IGNORES]", 0, 9);
			for (Long ignore : p.getIgnores().getIgnores()) {
				characterfile.write("character-ignore", 0, 19);
				characterfile.write(Long.toString(ignore), 0, Long.toString(ignore).length());
			}

			characterfile.write("[PRESETS]");

			characterfile.write("Names");
			Map<Integer, Preset> presets = p.getPresets().getPresets();
			for (Entry<Integer, Preset> entry : presets.entrySet()) {
				characterfile.write(entry.getValue().getAlias() + "\t");
			}
			for (Entry<Integer, Preset> entry : presets.entrySet()) {
				if (entry != null) {
					Preset preset = entry.getValue();
					PresetContainer inventory = preset.getInventory();
					characterfile.write("Inventory#" + entry.getKey() + "");
					for (Entry<Integer, GameItem> item : inventory.getItems().entrySet()) {
						characterfile.write(item.getKey() + "\t" + item.getValue().getId() + "\t" + item.getValue().getAmount() + "\t");
					}
					PresetContainer equipment = preset.getEquipment();
					characterfile.write("Equipment#" + entry.getKey() + "");
					for (Entry<Integer, GameItem> item : equipment.getItems().entrySet()) {
						characterfile.write(item.getKey() + "\t" + item.getValue().getId() + "\t" + item.getValue().getAmount() + "\t");
					}
				}
			}

			characterfile.write("[KILLSTREAKS]");
			for (Entry<Killstreak.Type, Integer> entry : p.getKillstreak().getKillstreaks().entrySet()) {
				characterfile.write(entry.getKey().name() + "" + entry.getValue());
			}

			characterfile.write("[TITLES]");
			for (Title title : p.getTitles().getPurchasedList()) {
				characterfile.write("title" + title.name());
			}

			characterfile.write("[NPC-TRACKER]");
			for (Entry<String, Integer> entry : p.getNpcDeathTracker().getTracker().entrySet()) {
				if (entry != null) {
					if (entry.getValue() > 0) {
						characterfile.write(entry.getKey().toString() + "" + entry.getValue());
					}
				}
			}
            */

			//characterfile.write("[EOF]", 0, 5);
		} catch (IOException ioexception) {
			Misc.println(p.playerName + ": error writing file.");
			if (Config.SERVER_STATE == ServerState.PRIVATE) {
				ioexception.printStackTrace();
			}
            p.sendMessage("Failed to save: failed to write to character file");
			return false;
		}
        p.sendMessage("Save successful");
		return true;
	}

}
