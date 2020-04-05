/*
 * Copyright (C) 2020 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.mcme.editor.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Eriol_Eandur
 */
public class Profiler {
    
    private static Map<String,Profile> profiles = new HashMap<>();
    
    private static long lastUpdate;
    
    private static long updateInterval = 1000000000;
    
    @Setter
    @Getter
    private static boolean enabled = false;
    
    public static void start(String name) {
        if(enabled) {
            Profile profile = profiles.get(name);
            if(profile==null) {
                profile = new Profile();
                profiles.put(name,profile);
            }
            if(profile.enabled && profile.updateStart) {
                profile.start = System.nanoTime();
                profile.updateStart = false;
            }
        }
    }
    
    public static void stop(String name) {
        if(enabled) {
            Profile profile = profiles.get(name);
            if(profile!=null) {
                if(profile.enabled) {
                    if(profile.updateStop) {
                        profile.stop = System.nanoTime();
                        profile.updateStop = false;
                        Logger.getGlobal().log(Level.INFO, "Profiler - {0}: {1} ms ({2})", new Object[]{name, (profile.stop-profile.start)/1e6,profile.counter});
                        profile.counter = 0;
                    } else {
                        profile.counter++;
                    }
                }
            }
        }
    }
    
    public static void update() {
//Logger.getGlobal().info("getUpdate "+profiles.size());
        if(enabled && System.nanoTime()>lastUpdate+updateInterval) {
            profiles.forEach((name,profile) -> {
                profile.updateStart = true;
                profile.updateStop = true;
            });
            lastUpdate = System.nanoTime();
        }
    }
    
    public static void enableComponent(String key, boolean enable) {
        Profile profile = profiles.get(key);
        if(profile!=null) {
            profile.enabled = enable;
        }
    }
    
    private static class Profile {
        
        public long start,stop;
        boolean updateStart, updateStop,enabled=true;
        public int counter;
        
    }
}
