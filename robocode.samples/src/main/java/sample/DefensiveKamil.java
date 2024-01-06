/*
 * Copyright (c) 2001-2023 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package sample;


import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.core.RuleBuilder;
import robocode.*;
import robocode.Robot;

import java.awt.*;


/**
 * DefensiveKamil - runs away, wins when other robots have no energy
 */
public class DefensiveKamil extends Robot {

    Facts spottedFacts = new Facts();
    Facts wallFacts = new Facts();
    Facts collisionFacts = new Facts();

    RulesEngine rulesEngine = new DefaultRulesEngine();
    Rules rules = new Rules();

    public double getMaxDistance() {
        return Math.sqrt(Math.pow(getBattleFieldWidth(), 2) + Math.pow(getBattleFieldHeight(), 2));
    }

    Rule changeColorRule = new RuleBuilder()
            .name("run away rule")
            .description("if you scan robot run from it")
            .when(facts -> facts.get("spotted").equals(true))
            .then(facts -> setBodyColor(Color.black))
            .build();

    Rule runAwayRule = new RuleBuilder()
            .name("run away")
            .description("if you scan robot run from it")
            .when(facts ->  facts.get("spotted").equals(true))
            .then(facts -> {
                double bearing = facts.get("bearing");
                double distance = facts.get("distance");
                turnRight(bearing + 180);
                back((getMaxDistance() * 0.1) - distance);
                turnGunLeft(bearing + 230);
            })
            .build();

    Rule wallCollision = new RuleBuilder()
            .name("wall collision")
            .description("on wall collision move forward")
            .when(facts ->  facts.get("collision").equals(true))
            .then(facts -> {
                turnLeft(20);
                ahead(300);
            })
            .build();

    Rule robotCollision = new RuleBuilder()
            .name("robot collision")
            .description("on robot collision change direction and run")
            .when(facts ->  facts.get("collision").equals(true))
            .then(facts -> {
                turnLeft(180);
                back(300);
            })
            .build();


    public void run() {
        rules.register(changeColorRule);
        rules.register(runAwayRule);
        rules.register(wallCollision);
        rules.register(robotCollision);

        setBodyColor(Color.red);
        setGunColor(Color.red);
        setRadarColor(Color.red);
        setScanColor(Color.red);
        setBulletColor(Color.red);

        while (true) {
            turnGunRight(5);
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        spottedFacts.put("spotted", true);
        spottedFacts.put("bearing", e.getBearing());
        spottedFacts.put("distance", e.getDistance());
        rulesEngine.fire(rules, spottedFacts);
        scan();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        scan();
    }

    public void onHitWall(HitWallEvent e) {
        wallFacts.put("collision", true);
        rulesEngine.fire(rules, wallFacts);
    }

    public void onHitRobot(HitRobotEvent e) {
        collisionFacts.put("collision", true);
        rulesEngine.fire(rules, collisionFacts);
    }
}
