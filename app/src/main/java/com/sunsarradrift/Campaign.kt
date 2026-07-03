package com.sunsarradrift

/**
 * THE CAMPAIGN — twelve missions retracing the Pale Blue tour route
 * (Earth → Moon → Venus → Sun → Belt → Jupiter → Saturn → Rama derelict →
 * Kuiper → Orion/TRAPPIST → Sagittarius A*), each one an open homage to a
 * classic space mission from film, print, tabletop or games (named in
 * `derivedFrom`, celebrated in the mission log).
 *
 * Story spine: Sunsarra Vex hunts the Confederate Empire, which kidnapped her
 * mother — the geneticist whose work demolishes their eugenics creed. Her crew:
 * Chen Yumei (mechanic, Luna-born, builds hope out of scrap) and Dr. Adaeze Obi
 * (evolutionary biologist who wields Gould, Lewontin and Cavalli-Sforza like
 * weapons). Le Guin gives the empire its walls, Sagan the finale its dot,
 * Asimov the long game, Clarke the thing sleeping past Saturn.
 */
object Campaign {

    class Mission(
        val id: String,
        val act: Int,
        val title: String,
        val role: Int,                    // Game.ROLE_*
        val u: Float,                     // rail anchor in SolarSystem units
        val driftU: Float,                // slow forward drift during play
        val derivedFrom: String,          // the homage
        val musicKey: String,             // assets/music/<musicKey>.mp3
        val briefing: String,
        val sceneIn: List<String>,        // script.json line ids
        val sceneOut: List<String>,
        val goal: Int,                    // kills / gates / (transfers*10+seconds/10)
        val intensity: Float              // spawn/fault/pursuit rate scale
    )

    val missions = listOf(
        Mission("m01", 1, "EMBERS OF FREEPORT", Game.ROLE_GUNNER, 0.25f, 0.002f,
            "Battlestar Galactica's cold-open ambush · the Millennium Falcon's Tatooine escape",
            "act1_battle",
            "The Empire hit Freeport Station looking for your mother's files.\nMan the turret. Cover the refugee pods.\nDestroy 12 interceptors.",
            listOf("m01_in1", "m01_in2", "m01_in3"),
            listOf("m01_out1", "m01_out2"),
            12, 1.0f),
        Mission("m02", 1, "THE LONG FALL", Game.ROLE_PILOT, 2.0f, 0.010f,
            "The Death Star trench run · Atari's Lunar Lander, all grown up",
            "act1_run",
            "Mom hid a data cache in the Mare Crisium relay canyons.\nFly the trench. Thread 14 marker gates.\nThe lance behind you does not get tired.",
            listOf("m02_in1", "m02_in2"),
            listOf("m02_out1", "m02_out2", "m02_out3"),
            14, 1.0f),
        Mission("m03", 2, "THIRTEEN BREATHS", Game.ROLE_ENGINEER, 2.5f, 0.001f,
            "Apollo 13 — 'we've never lost an American in space, and we won't on my watch'",
            "act2_tension",
            "Refugee freighter KESTREL is dying: fires, leaks, frozen pumps.\nKeep her alive 150 seconds and push 3 fuel transfers across.\nStay calm. Calm is a tool.",
            listOf("m03_in1", "m03_in2", "m03_in3"),
            listOf("m03_out1", "m03_out2"),
            3 * 1000 + 150, 0.9f),
        Mission("m04", 2, "CLOUD MIRROR", Game.ROLE_GUNNER, 3.0f, 0.002f,
            "Cloud City's evacuation · Wing Commander escort duty",
            "act1_battle",
            "Venus cloud-cities are scattering before the Selector fleet.\nEscort the liner VESPER GLASS. Destroy 16 raiders.\nShe cannot take another torpedo.",
            listOf("m04_in1", "m04_in2"),
            listOf("m04_out1", "m04_out2"),
            16, 1.15f),
        Mission("m05", 3, "CORONA RUN", Game.ROLE_PILOT, 4.0f, 0.012f,
            "Sunshine's terrible daylight · the Mutara Nebula gambit, flown at the Sun",
            "act3_inferno",
            "Only one road past the blockade: through the corona itself.\nRide the flare channels — 12 thermal gates.\nMiss one and the Sun will notice you.",
            listOf("m05_in1", "m05_in2"),
            listOf("m05_out1", "m05_out2", "m05_out3"),
            12, 1.2f),
        Mission("m06", 3, "THE QUIET FOUNDRY", Game.ROLE_ENGINEER, 6.0f, 0.001f,
            "FTL's burning crew decks · the Canterbury's last shift (The Expanse)",
            "act2_tension",
            "Belt foundry NADEZHDA sheltered you — and Empire sappers lit it up.\nHold her together 180 seconds, 2 fuel transfers to the escape tugs.\nAda is cracking their 'fitness ledger' while you work.",
            listOf("m06_in1", "m06_in2", "m06_in3"),
            listOf("m06_out1", "m06_out2", "m06_out3"),
            2 * 1000 + 180, 1.1f),
        Mission("m07", 4, "KINGFALL AMBUSH", Game.ROLE_GUNNER, 7.0f, 0.003f,
            "BSG '33' — they just keep coming · Ender's Game swarm geometry",
            "act1_battle",
            "They tracked the convoy through Jupiter's radiation shadow.\nWaves without end. Destroy 22 and the jump computers finish.\nConserve the EMP for the clusters.",
            listOf("m07_in1", "m07_in2"),
            listOf("m07_out1", "m07_out2"),
            22, 1.35f),
        Mission("m08", 4, "RINGRUNNER", Game.ROLE_PILOT, 9.0f, 0.011f,
            "2001's silent approach · Star Fox's corridor, threaded through Saturn's rings",
            "act1_run",
            "Lose the Selector lances inside the B-ring.\n16 gates between the icebergs. Boost on the straights.\nYu says the hull can take it. Yu is usually right.",
            listOf("m08_in1", "m08_in2"),
            listOf("m08_out1", "m08_out2"),
            16, 1.3f),
        Mission("m09", 5, "THE SILENT CYLINDER", Game.ROLE_ENGINEER, 10.5f, 0.0f,
            "Rendezvous with Rama — the artifact does not care that you are impressed",
            "act5_wonder",
            "A dead cylinder older than the Sun, parked past Iapetus.\nWake its systems: 200 seconds of power-routing, 2 conduit transfers.\nWhatever built it never met a 'perfect' genome. It built anyway.",
            listOf("m09_in1", "m09_in2", "m09_in3"),
            listOf("m09_out1", "m09_out2", "m09_out3"),
            2 * 1000 + 200, 0.85f),
        Mission("m10", 5, "GATES OF ICE", Game.ROLE_GUNNER, 13.0f, 0.002f,
            "Traveller RPG patrol duty · Homeworld's Kharak defense, in miniature",
            "act1_battle",
            "The Kuiper listening post heard your mother's voice — alive.\nThe Empire wants the post erased. 18 hostiles.\nCasque himself is on the long-range channel.",
            listOf("m10_in1", "m10_in2", "m10_in3"),
            listOf("m10_out1", "m10_out2"),
            18, 1.4f),
        Mission("m11", 6, "THE WORD FOR SKY", Game.ROLE_PILOT, 17.0f, 0.014f,
            "Le Guin's walls coming down · FTL's last jump, through a stellar nursery",
            "act6_liberation",
            "The genebank convoy — thousands the Empire labeled 'unfit' —\nbreaks for TRAPPIST-1 tonight. Fly point through the Orion shoals.\n18 gates. Every one you hit, a colony ship lives.",
            listOf("m11_in1", "m11_in2", "m11_in3"),
            listOf("m11_out1", "m11_out2", "m11_out3"),
            18, 1.45f),
        Mission("m12", 7, "ASCENSION ENGINE", Game.ROLE_GUNNER, 22.4f, 0.004f,
            "The reactor run at Endor · 2010's last transmission · Foundation's victory-by-idea",
            "act7_finale",
            "Casque's Ascension Engine hangs at the event horizon — your mother inside.\nShoot ONLY the emitter pylons: 14 precise kills, zero station hits.\nAda and Maren have a broadcast ready. Win them the time.",
            listOf("m12_in1", "m12_in2", "m12_in3"),
            listOf("m12_out1", "m12_out2", "m12_out3", "m12_out4", "m12_out5"),
            14, 1.5f)
    )

    // shared radio pools (mission engine picks by event)
    val radioWave = listOf("r_wave1", "r_wave2", "r_wave3")
    val radioLowShield = listOf("r_low1", "r_low2", "r_low3")
    val radioKillStreak = listOf("r_streak1", "r_streak2")
    val radioGate = listOf("r_gate1", "r_gate2")
    val radioMiss = listOf("r_miss1", "r_miss2")
    val radioRepair = listOf("r_fix1", "r_fix2")
    val radioWin = listOf("r_win1", "r_win2", "r_win3")
    val radioFail = listOf("r_fail1", "r_fail2")
}
