/*
 * Copyright (c) 2001-2023 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package sample;


import org.jeasy.rules.api.*;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.core.RuleBuilder;
import robocode.*;

import java.awt.geom.Point2D;

public class Hybrid extends AdvancedRobot {
    Rules rules = new Rules();
    Facts fireFacts = new Facts();
    Facts movementFacts = new Facts();
    Facts movementFacts2 = new Facts();

    Facts runAwayFacts = new Facts();

    Facts wallFacts = new Facts();


    Rule fireRule = new RuleBuilder()
            .name("fire")
            .description("fire a shot if you are in position")
            .when(facts -> facts.get("fire").equals(true))
            .then(facts -> {
                ScannedRobotEvent event = facts.get("event");
                this.predict_shot(3.0, event);
            })
            .build();

    Rule movementRule = new RuleBuilder()
            .name("movement")
            .description("setBack")
            .when(facts -> facts.get("setBack").equals(true))
            .then(facts -> {
                double distance = facts.get("distance");
                setBack(distance);
            })
            .build();

    Rule movementRule2 = new RuleBuilder()
            .name("movement2")
            .description("setAhead")
            .when(facts -> facts.get("setAhead").equals(true))
            .then(facts -> {
                double distance = facts.get("distance");
                setAhead(distance);
            }).build();

    Rule wallCollision = new RuleBuilder()
            .name("wall collision")
            .description("on wall collision move forward")
            .when(facts ->  facts.get("collision").equals(true))
            .then(facts -> {
                turnLeft(20);
                ahead(300);
            })
            .build();


    public double getMaxDistance() {
        return Math.sqrt(Math.pow(getBattleFieldWidth(), 2) + Math.pow(getBattleFieldHeight(), 2));
    }
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

    RulesEngine rulesEngine = new DefaultRulesEngine();

    public void run() {
        rules.register(fireRule);
        rules.register(movementRule);
        rules.register(movementRule2);
        rules.register(runAwayRule);
        rules.register(wallCollision);
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);
        setTurnRadarRight(1000); // initial scan
        execute();
        while (true) {
            // if we stopped moving the radar, move it a tiny little bit
            // so we keep generating scan events
            if (getRadarTurnRemaining() == 0)
                setTurnRadarRight(3);
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        // turn toward the robot we scanned
        int energyTrigger = 15;


        if (getEnergy() <= energyTrigger) {
            runAwayFacts.put("spotted", true);
            runAwayFacts.put("bearing", e.getBearing());
            runAwayFacts.put("distance", e.getDistance());
            rulesEngine.fire(rules, runAwayFacts);
        } else {
            setTurnRight(normalizeBearing(e.getBearing()));

            // normalize the turn to take the shortest path there
            setTurnGunRight(normalizeBearing(e.getBearing()));
            execute();
            boolean setMovement = false;
            // if we've turned toward our enemy...
            if (Math.abs(getTurnRemaining()) < 10) {
                if (e.getDistance() > 300) {
                    movementFacts2.put("setAhead", true);
                    movementFacts2.put("distance", e.getDistance() / 2);
                    rulesEngine.fire(rules, movementFacts2);
//                setAhead(e.getDistance() / 2);
                    setMovement = true;
                } else if (e.getDistance() < 100) {
                    movementFacts.put("setBack", true);
                    movementFacts.put("distance", e.getDistance() * 2);
                    rulesEngine.fire(rules, movementFacts);
//                setBack(e.getDistance() * 2);
                }
                fireFacts.put("event", e);
                fireFacts.put("fire", true);
                rulesEngine.fire(rules, fireFacts);
//            this.predict_shot(3.0, e);
                execute();
            }
            // lock our radar onto our target
            setTurnRadarRight(getHeading() - getRadarHeading() + e.getBearing());
        }
    }


    public void onHitWall(HitWallEvent e) {
        wallFacts.put("collision", true);
        rulesEngine.fire(rules, wallFacts);
    }
    // normalizes a bearing to between +180 and -180
    double normalizeBearing(double angle) {
        while (angle >  180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
    // if the robot we were shooting at died, scan around again
    public void onRobotDeath(RobotDeathEvent e) { setTurnRadarRight(1000); }
    public void predict_shot(Double firePower, ScannedRobotEvent enemy) {
        // calculate speed of bullet
        double bulletSpeed = 20 - firePower * 3;
        // distance = rate * time, solved for time
        long time = (long)(enemy.getDistance() / bulletSpeed);

        // calculate gun turn to predicted x,y location
        double futureX = getFutureX(time, enemy);
        double futureY = getFutureY(time, enemy);
        double absDeg = absoluteBearing(getX(), getY(), futureX, futureY);

        // turn the gun to the predicted x,y location
        setTurnGunRight(normalizeBearing(absDeg - getGunHeading()));

        setFire(firePower);
    }

    // computes the absolute bearing between two points
    double absoluteBearing(double x1, double y1, double x2, double y2) {
        double xo = x2-x1;
        double yo = y2-y1;
        double hyp = Point2D.distance(x1, y1, x2, y2);
        double arcSin = Math.toDegrees(Math.asin(xo / hyp));
        double bearing = 0;

        if (xo > 0 && yo > 0) { // both pos: lower-Left
            bearing = arcSin;
        } else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
            bearing = 360 + arcSin; // arcsin is negative here, actually 360 - ang
        } else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
            bearing = 180 - arcSin;
        } else if (xo < 0 && yo < 0) { // both neg: upper-right
            bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
        }

        return bearing;
    }
    public double getFutureX(long when, ScannedRobotEvent e){
        double absBearingDeg = (this.getHeading() + e.getBearing());
        if (absBearingDeg < 0) absBearingDeg += 360;
        double x = this.getX() + Math.sin(Math.toRadians(absBearingDeg)) * e.getDistance();
        return x + Math.sin(Math.toRadians(getHeading())) * getVelocity() * when;
    }

    public double getFutureY(long when ,ScannedRobotEvent e){
        double absBearingDeg = (this.getHeading() + e.getBearing());
        if (absBearingDeg < 0) absBearingDeg += 360;
        double y = this.getY() + Math.cos(Math.toRadians(absBearingDeg)) * e.getDistance();
        return y + Math.cos(Math.toRadians(getHeading())) * getVelocity() * when;
    }
}
