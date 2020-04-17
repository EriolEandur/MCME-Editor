/*
 * Copyright (C) 2019 MCME
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
package com.mcmiddleearth.mcme.editor.command;

import com.mcmiddleearth.mcme.editor.EditorPlugin;
import com.mcmiddleearth.mcme.editor.Permissions;
import com.mcmiddleearth.mcme.editor.command.argument.StringArgument;
import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.command.sender.EditPlayer;
import com.mcmiddleearth.mcme.editor.job.AbstractJob;
import com.mcmiddleearth.mcme.editor.job.JobManager;
import com.mcmiddleearth.mcme.editor.job.JobStatus;
import com.mcmiddleearth.mcme.editor.queue.QueueConfiguration;
import com.mcmiddleearth.mcme.editor.util.Profiler;
import com.mcmiddleearth.mcme.editor.util.ProgressMessenger;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Eriol_Eandur
 */
public class QueueCommand extends AbstractEditorCommand {

    @Override
    public LiteralArgumentBuilder<EditCommandSender> getCommandTree() {
        return LiteralArgumentBuilder.<EditCommandSender>literal("queue")
            .requires(s -> s.hasPermissions(Permissions.QUEUE))
            .executes(c -> {
                (c.getSource()).info("See manual at...");
                return 0;})
            .then(LiteralArgumentBuilder.<EditCommandSender>literal("list")
                .executes(c -> {return displayJobs(c,1,"");})
                .then(RequiredArgumentBuilder.<EditCommandSender,Integer>argument("<page>", integer())
                    .executes(c -> {
                        int page = c.getArgument("<page>", Integer.class);
                        return displayJobs(c,page,"");})
                    .then(RequiredArgumentBuilder.<EditCommandSender,String>argument("<job states>", word())
                        .executes(c -> {
                            String states = c.getArgument("<job states>",String.class);
                            int page = c.getArgument("<page>", Integer.class);
                            return displayJobs(c,page,states);})))
                .then(RequiredArgumentBuilder.<EditCommandSender,String>argument("<job states>", word())
                    .executes(c -> {
                        String states = c.getArgument("<job states>",String.class);
                        return displayJobs(c,1,states);})))
            .then(LiteralArgumentBuilder.<EditCommandSender>literal("restart")
                .requires(s -> s.hasPermissions(Permissions.QUEUE_RESTART))
                .executes(c -> {
                    return restartQueue(c.getSource());
                }))
            .then(LiteralArgumentBuilder.<EditCommandSender>literal("config")
                .requires(s -> s.hasPermissions(Permissions.QUEUE_CONFIG))
                .then(LiteralArgumentBuilder.<EditCommandSender>literal("show")
                    .executes(c -> {
                        return showConfiguration(c.getSource());
                    }))
                .then(RequiredArgumentBuilder.<EditCommandSender,String>argument("key",new StringArgument(QueueConfiguration.Key.getKeys()))
                    .then(RequiredArgumentBuilder.<EditCommandSender,Integer>argument("value", integer())
                        .executes(c -> {
                            return setConfiguration(c.getSource(), c.getArgument("key", String.class),c.getArgument("value",Integer.class));
                    }))))
            .then(LiteralArgumentBuilder.<EditCommandSender>literal("debug")
                .requires(s -> s.hasPermissions(Permissions.QUEUE_CONFIG))
                .then(RequiredArgumentBuilder.<EditCommandSender,String>argument("enabled",new StringArgument("on","off"))
                    .executes(c -> {
                        return setProfiler(c.getSource(),c.getArgument("enabled", String.class),"");
                    })
                    .then(RequiredArgumentBuilder.<EditCommandSender,String>argument("component", word())
                        .executes(c -> {
                            setProfiler(c.getSource(),c.getArgument("enabled", String.class),c.getArgument("component",String.class));
                            return 0;
                        }))))
            .then(LiteralArgumentBuilder.<EditCommandSender>literal("updates")
                .requires(s -> s.hasPermissions(Permissions.QUEUE_UPDATES))
                .then(RequiredArgumentBuilder.<EditCommandSender,String>argument("enabled",new StringArgument("on","off"))
                    .executes(c -> {
                        return setUpdates(c.getSource(),c.getArgument("enabled", String.class));
                    })))
            .then(LiteralArgumentBuilder.<EditCommandSender>literal("suspend")
                .requires(s -> s.hasPermissions(Permissions.QUEUE_SUSPEND))
                .executes(c -> {return suspendJob(c,false);})
                .then(RequiredArgumentBuilder.<EditCommandSender,Integer>argument("<jobID>", integer())
                    .executes(c -> {return suspendJob(c,false);}))
                .then(LiteralArgumentBuilder.<EditCommandSender>literal("-a")
                    .requires(s -> s.hasPermissions(Permissions.QUEUE_OTHER))
                    .executes(c -> {return suspendJob(c,true);})))
            .then(LiteralArgumentBuilder.<EditCommandSender>literal("resume")
                .requires(s -> s.hasPermissions(Permissions.QUEUE_RESUME))
                .executes(c -> {return resumeJob(c,false);})
                .then(RequiredArgumentBuilder.<EditCommandSender,Integer>argument("<jobID>", integer())
                    .executes(c -> {return resumeJob(c,false);}))
                .then(LiteralArgumentBuilder.<EditCommandSender>literal("-a")
                    .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.QUEUE_OTHER))
                    .executes(c -> {return resumeJob(c,true);})))
            .then(LiteralArgumentBuilder.<EditCommandSender>literal("cancel")
                .requires(s -> s.hasPermissions(Permissions.QUEUE_CANCEL))
                .executes(c -> {return cancelJob(c,false);})
                .then(RequiredArgumentBuilder.<EditCommandSender,Integer>argument("<jobID>", integer())
                    .executes(c -> {return cancelJob(c,false);}))
                .then(LiteralArgumentBuilder.<EditCommandSender>literal("-a")
                    .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.QUEUE_OTHER))
                    .executes(c -> {return cancelJob(c,true);
                })))
            .then(LiteralArgumentBuilder.<EditCommandSender>literal("delete")
                .requires(s -> s.hasPermissions(Permissions.QUEUE_DELETE))
                .executes(c -> {return deleteJob(c,false);})
                .then(RequiredArgumentBuilder.<EditCommandSender,Integer>argument("<jobID>", integer())
                    .executes(c -> {return deleteJob(c,false);}))
                .then(LiteralArgumentBuilder.<EditCommandSender>literal("-a")
                    .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.QUEUE_OTHER))
                    .executes(c -> {return deleteJob(c,true);
                })));
    }
    
    private int displayJobs(CommandContext c, int page, String states) {
        Iterator<AbstractJob> jobs = JobManager.getJobs();
        List<String[]> jobStrings = new ArrayList<>();
        Set<JobStatus> selectedStates = getSelectedStates(states);
        EditCommandSender sender = (EditCommandSender) c.getSource();
        while(jobs.hasNext()) {
            AbstractJob job = jobs.next();
            if(!selectedStates.contains(job.getStatus())) {
                continue;
            }
            if(sender.hasPermissions(Permissions.QUEUE_OTHER)
                    || job.isOwner(sender)) {
                String ownerName = (job.getOwner() instanceof EditPlayer?
                                        ((EditPlayer)job.getOwner()).getOfflinePlayer().getName():
                                        "CONSOLE");
                jobStrings.add(new String[]{"ID: "+job.getId()+" ("+job.getStatus()+")\n"+ProgressMessenger.formatProgressMessage(job.progresMessage()),
                                            "Owner: "+ownerName+"\n"
                                            +"Started: "+LocalDateTime.ofEpochSecond(job.getStartTime()/1000,0, ZoneOffset.UTC)+"\n"
                                            +"Ended: "+(job.getEndTime()>0?
                                                        LocalDateTime.ofEpochSecond(job.getEndTime()/1000,0, ZoneOffset.UTC)+"\n":
                                                        "---\n")
                                            +"Type: "+job.getType()//+"\n"
                                            +job.getResultMessage()
                                            });
            }      
        }
        if(c.getSource() instanceof EditPlayer && (((EditPlayer)c.getSource()).isOnline())) {
            List<FancyMessage> messages = new ArrayList<>();
            jobStrings.forEach(msg -> messages.add(new FancyMessage(EditorPlugin.getMessageUtil())
                                                   .addTooltipped(msg[0],msg[1])));
            EditorPlugin.getMessageUtil()
                        .sendFancyListMessage(((EditPlayer)c.getSource()).getPlayer(), 
                                new FancyMessage(EditorPlugin.getMessageUtil())
                                        .addSimple("Current editor jobs:"), 
                                messages,"/queue list", page);
        } else {
            ((EditCommandSender)c.getSource()).info("Current editor jobs:");
            jobStrings.forEach(msg -> ((EditCommandSender)c.getSource()).info(msg[0]+"\n"+msg[1]));
        }
        return 0;
    }
    
    private Set<JobStatus> getSelectedStates(String selection) {
        boolean negation = false;
        Set<JobStatus> states = new HashSet<>();
        if(selection.equals("")) {
            states.addAll(Arrays.asList(JobStatus.values()));
            states.remove(JobStatus.FINISHED);
        } else if( selection.startsWith("!")) {
            states.addAll(Arrays.asList(JobStatus.values()));
        }
        for(int i = 0; i < selection.length(); i++) {
            if(selection.charAt(i)=='!') {
                negation = true;
                continue;
            }
            JobStatus status = null;
            switch(selection.charAt(i)) {
                case 'f':
                    status = JobStatus.FINISHED;
                    break;
                case 'F':
                    status = JobStatus.FAILED;
                    break;
                case 'c':
                    status = JobStatus.CANCELLED;
                    break;
                case 'C':
                    status = JobStatus.CREATION;
                    break;
                case 'w':
                    status = JobStatus.WAITING;
                    break;
                case 's':
                    status = JobStatus.SUSPENDED;
                    break;
                case 'r':
                    status = JobStatus.RUNNING;
                    break;
            }
            if(status!=null) {
                if(negation) {
                    states.remove(status);
                } else {
                    states.add(status);
                }
                negation = false;
            }
        }
        return states;
    }

    private int suspendJob(CommandContext c, boolean all) {
        int jobId = getJobId(c);
        EditCommandSender sender = (EditCommandSender)c.getSource();
        if(jobId==-1) {
            JobManager.suspendAllJobs(sender, all);
            if(all) {
                sender.info("Jobs of all players have been suspended.");
            } else {
                sender.info("Your jobs have been suspended.");
            }
        } else {
            if(sender.hasPermissions(Permissions.QUEUE_OTHER) 
                    || JobManager.getJob(jobId).getOwner().equals(sender)) {
                if(JobManager.getJob(jobId).isRunnable()) {
                    JobManager.suspendJob(sender, jobId);
                } else {
                    sender.error("This job can't be suspended.");
                }
            } else {
                sender.sendNoPermissionMessage();
            }
        }
        return 0;
    }

    private int resumeJob(CommandContext c, boolean all) {
        int jobId = getJobId(c);
        EditCommandSender sender = (EditCommandSender)c.getSource();
        if(jobId==-1) {
            JobManager.resumeAllJobs(sender, all);
            if(all) {
                sender.info("Jobs of all players have been resumed.");
            } else {
                sender.info("Your jobs have been resumed.");
            }
        } else {
            if(sender.hasPermissions(Permissions.QUEUE_OTHER) 
                    || JobManager.getJob(jobId).getOwner().equals(sender)) {
                if(JobManager.getJob(jobId).isRunnable()) {
                    JobManager.resumeJob(sender, jobId);
                } else {
                    sender.error("This job can't be resumed.");
                }
            } else {
                sender.sendNoPermissionMessage();
            }
        }
        return 0;
    }

    private int cancelJob(CommandContext c, boolean all) {
        int jobId = getJobId(c);
        EditCommandSender sender = (EditCommandSender)c.getSource();
        if(jobId==-1) {
            JobManager.cancelAllJobs(sender, all);
            if(all) {
                sender.info("Jobs of all players have been cancelled.");
            } else {
                sender.info("Your jobs have been cancelled.");
            }
        } else {
            if(sender.hasPermissions(Permissions.QUEUE_OTHER) 
                    || JobManager.getJob(jobId).getOwner().equals(sender)) {
                if(JobManager.getJob(jobId).isRunnable()) {
                    JobManager.cancelJob(sender, jobId);
                } else {
                    sender.error("This job can't be cancelled.");
                }
            } else {
                sender.sendNoPermissionMessage();
            }
        }
        return 0;
    }
    
    private int deleteJob(CommandContext c, boolean all) {
        int jobId = getJobId(c);
        EditCommandSender sender = (EditCommandSender)c.getSource();
        if(jobId==-1) {
            JobManager.dequeueAllJobs(sender, all);
            if(all) {
                sender.info("Deletable jobs of all players have been delete.");
            } else {
                sender.info("Your deletable jobs have been deleted.");
            }
        } else {
            if(sender.hasPermissions(Permissions.QUEUE_OTHER) 
                    || JobManager.getJob(jobId).getOwner().equals(sender)) {
                if(JobManager.getJob(jobId).isDequeueable()) {
                    JobManager.dequeueJob(jobId);
                } else {
                    sender.error("This job can't be deleted. You need to cancel it first.");
                }
            } else {
                sender.sendNoPermissionMessage();
            }
        }
        return 0;
    }

    private int getJobId(CommandContext c) {
        try {
            return (Integer) c.getArgument("<jobID>", Integer.class);
        } catch(IllegalArgumentException ex) {
            return -1;
        }
    }
    
    private int setConfiguration(EditCommandSender c, String key, int value) {
        QueueConfiguration.Key queueKey = QueueConfiguration.Key.valueOf(key);
        if(queueKey.getMinValue() <= value && value <= queueKey.getMaxValue()) {
            QueueConfiguration.set(queueKey,value);
            c.info("Set queue config key "+key+" = "+value);
        } else {
            c.error("Invalid value. Range for key "+key+" is ["+queueKey.getMinValue()+" ; "+queueKey.getMaxValue()+"].");
        }
        return 0;
    }
    
    private int showConfiguration(EditCommandSender c) {
        c.info("Queue configuration:");
        for(QueueConfiguration.Key key: QueueConfiguration.Key.values()) {
            c.info("- "+key+" = "+ QueueConfiguration.get(key));
        }
        return 0;
    }

    private int restartQueue(EditCommandSender source) {
        source.info("Restarting Editor Queue, this will take about 10 seconds...");
        JobManager.restartJobScheduler(source);
        /*new BukkitRunnable() {
            @Override
            public void run() {
                JobManager.startJobScheduler();
                source.info("Editor Queue started.");
            }
        }.runTaskLater(EditorPlugin.getInstance(),200);*/
        return 0;
    }
    
    private int setUpdates(EditCommandSender source, String arg) {
        Iterator<AbstractJob> iterator = JobManager.getJobs();
        boolean enable = arg.equalsIgnoreCase("on");
        while(iterator.hasNext()) {
            AbstractJob job = iterator.next();
            if(job.getOwner().equals(source)) {
                job.setSendUpdates(enable);
            }
        }
        return 0;
    }
    
    private int setProfiler(EditCommandSender source, String enabled, String component) {
        if(component.equals("")) {
            Profiler.setEnabled(enabled.equalsIgnoreCase("on"));
            source.info("Profiling "+(Profiler.isEnabled()?"enabled.":"disabled."));
        } else {
            Profiler.enableComponent(component, enabled.equalsIgnoreCase("on"));
            source.info("Profiling of component '"+component+"' "+(enabled.equalsIgnoreCase("on")?"enabled.":"disabled."));
        }
        return 0;
    }
            
}
