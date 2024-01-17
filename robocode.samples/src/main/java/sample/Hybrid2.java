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

import java.awt.*;

import static robocode.util.Utils.normalRelativeAngleDegrees;


/**
 * DefensiveKamil - hides in corner and fire, moves to another each time energy dropped by 25%
 */
public class Hybrid2 extends AdvancedRobot {
    boolean stopWhenSeeRobot = false;
    static int corner = 0;
    int energyTrigger;

    Facts fireFacts = new Facts();
    Facts energyFacts = new Facts();
    Facts collisionFacts = new Facts();

    RulesEngine rulesEngine = new DefaultRulesEngine();
    Rules rules = new Rules();

    Rule fireRule = new RuleBuilder()
            .name("fire")
            .description("if you scan robot fire depending on distance")
            .when(facts -> facts.get("spotted").equals(true))
            .then(facts -> {
                double robotDistance = facts.get("robotDistance");
                this.smartFire(robotDistance);
            })
            .build();

    Rule energyRules = new RuleBuilder()
            .name("energy drop")
            .description("on energy drop by 25% change corener")
            .when(facts ->  facts.get("energytrigger").equals(true))
            .then(facts -> {
                energyTrigger -= 25;
                corner += 90;
                if (corner == 270) {
                    corner = -90;
                }
                turnGunRight(90);
                goCorner();
            })
            .build();


    public void goCorner() {
        turnRight(normalRelativeAngleDegrees(corner - getHeading()));
        ahead(5000);
        turnLeft(90);
        ahead(5000);
        turnGunLeft(90);
    }


    public void run() {
        rules.register(fireRule);
        rules.register(energyRules);

        energyTrigger = 75;
        setBodyColor(Color.black);
        setGunColor(Color.black);
        setRadarColor(Color.black);
        setScanColor(Color.black);
        setBulletColor(Color.black);

        addCustomEvent(new Condition("energytrigger") {
            public boolean test() {
                return (getEnergy() <= energyTrigger);
            }
        });

        goCorner();

        int gunIncrement = 5;
        while (true) {
            for (int i = 0; i < 18; i++) {
                turnGunLeft(gunIncrement);
            }
            gunIncrement *= -1;
        }
    }

    public void onDeath(DeathEvent e) {
        corner += 90;
        if (corner == 270) {
            corner = -90;
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        fireFacts.put("spotted", true);
        fireFacts.put("robotDistance", e.getDistance());
        rulesEngine.fire(rules, fireFacts);
    }

    public void smartFire(double robotDistance) {
        if (robotDistance > 200 || getEnergy() < 15) {
            fire(1);
        } else if (robotDistance > 50) {
            fire(2);
        } else {
            fire(3);
        }
    }

    public void onCustomEvent(CustomEvent e) {
        if (e.getCondition().getName().equals("energytrigger")) {
            energyFacts.put("energytrigger", true);
            rulesEngine.fire(rules, energyFacts);
        }
    }

}
