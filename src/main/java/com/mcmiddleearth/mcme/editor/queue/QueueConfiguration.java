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
package com.mcmiddleearth.mcme.editor.queue;

import com.mcmiddleearth.mcme.editor.EditorPlugin;
import com.mcmiddleearth.mcme.editor.job.AbstractJob;
import com.mcmiddleearth.mcme.editor.tasks.SyncJobScheduler;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

/**
 *
 * @author Eriol_Eandur
 */
public class QueueConfiguration {
    
    public static final String QUEUE_CONFIG = "queueConfig";
    
    public static void set(QueueConfiguration.Key key, int value) {
        setWithoutSaving(key,value);
        saveConfig();
    }
    
    private static void setWithoutSaving(QueueConfiguration.Key key, int value) {
        switch(key) {
            case QUEUE_SIZE:
                ReadingQueue.setTaskSize(value);
                break;
            case MAX_CHUNK_TICKETS:
                AbstractJob.setMaxChunkTickets(value);
                break;
            case SERVER_TIME_AVERAGE:
                SyncJobScheduler.setServerTimeAverage(value);
                break;
            case SERVER_TIME_FULL_WORK:
                SyncJobScheduler.setServerTimeFullWork(value);
                break;
            case SERVER_TIME_HALF_WORK:
                SyncJobScheduler.setServerTimeHalfWork(value);
                break;
            case FULL_WORK_TIME:
                SyncJobScheduler.setFullWorkTime(value);
                break;
            case HALF_WORK_TIME:
                SyncJobScheduler.setHalfWorkTime(value);
                break;
        }
    }
    
    public static int get(QueueConfiguration.Key key) {
        switch(key) {
            case QUEUE_SIZE:
                return ReadingQueue.getTaskSize();
            case MAX_CHUNK_TICKETS:
                return AbstractJob.getMaxChunkTickets();
            case SERVER_TIME_AVERAGE:
                return SyncJobScheduler.getServerTimeAverage();
            case SERVER_TIME_FULL_WORK:
                return (int) SyncJobScheduler.getServerTimeFullWork();
            case SERVER_TIME_HALF_WORK:
                return (int) SyncJobScheduler.getServerTimeHalfWork();
            case FULL_WORK_TIME:
                return (int) SyncJobScheduler.getFullWorkTime();
            case HALF_WORK_TIME:
                return (int) SyncJobScheduler.getHalfWorkTime();
        }
        return 0;
    }
    
    public static void saveConfig() {
        ConfigurationSection config = EditorPlugin.getInstance().getConfig().getConfigurationSection(QUEUE_CONFIG);
        if(config == null) {
            config = EditorPlugin.getInstance().getConfig().createSection(QUEUE_CONFIG);
        }
        for(QueueConfiguration.Key key: QueueConfiguration.Key.values()) {
            config.set(key.name(), get(key));
        }
        EditorPlugin.getInstance().saveConfig();
    }
    
    public static void loadConfig() {
        ConfigurationSection config = EditorPlugin.getInstance().getConfig().getConfigurationSection(QUEUE_CONFIG);
        if(config != null) {
            for(QueueConfiguration.Key key: QueueConfiguration.Key.values()) {
                setWithoutSaving(key,config.getInt(key.name()));
            }
        }
    }
        
    public enum Key {
        QUEUE_SIZE              (1, 100),
        MAX_CHUNK_TICKETS       (0, 1000),
        SERVER_TIME_AVERAGE     (1, 1000),
        SERVER_TIME_FULL_WORK   ((int)50e6, (int)100e6),
        SERVER_TIME_HALF_WORK   ((int)50e6, (int)100e6),  
        FULL_WORK_TIME          ((int)1e6, (int) 50e6),
        HALF_WORK_TIME          ((int)1e6, (int) 50e6);
        
        @Getter
        private final int minValue;
        @Getter 
        private final int maxValue;
        
        private Key(int minValue, int maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
        
        public static String[] getKeys() {
            Key[] list = values();
            String[] result = new String[list.length];
            for(int i = 0; i< list.length; i++) {
                result[i] = list[i].name();
            }
            return result;
        }
    }
}
