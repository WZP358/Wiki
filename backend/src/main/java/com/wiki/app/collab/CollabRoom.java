package com.wiki.app.collab;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class CollabRoom {
    private final Long docId;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, CollabParticipant> participants = new ConcurrentHashMap<>();
    private final Map<Integer, String> versionContent = new ConcurrentHashMap<>();
    private final Map<Integer, OtTextOperation> versionOps = new ConcurrentHashMap<>();

    private String title;
    private String content;
    private int version;

    public CollabRoom(Long docId, String title, String content, int version) {
        this.docId = docId;
        this.title = title;
        this.content = content;
        this.version = version;
        this.versionContent.put(version, content == null ? "" : content);
    }

    public void update(String title, String content, int version, OtTextOperation op) {
        this.title = title;
        this.content = content;
        this.version = version;
        this.versionContent.put(version, content == null ? "" : content);
        if (op != null) {
            this.versionOps.put(version, op);
        }
        if (this.versionContent.size() > 120) {
            int min = this.versionContent.keySet().stream().min(Integer::compareTo).orElse(version);
            this.versionContent.remove(min);
            this.versionOps.remove(min);
        }
    }
}
