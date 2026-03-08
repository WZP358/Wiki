package com.wiki.app.collab;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtTextOperation {
    private int position;
    private int deleteCount;
    private String insertText;

    public String apply(String base) {
        String safe = base == null ? "" : base;
        if (position < 0 || position > safe.length()) {
            throw new IllegalArgumentException("invalid position");
        }
        if (deleteCount < 0 || position + deleteCount > safe.length()) {
            throw new IllegalArgumentException("invalid delete count");
        }
        String insert = insertText == null ? "" : insertText;
        return safe.substring(0, position) + insert + safe.substring(position + deleteCount);
    }

    public OtTextOperation transformAgainst(OtTextOperation remote) {
        if (remote == null) {
            return new OtTextOperation(position, deleteCount, insertText);
        }

        int localStart = position;
        int localEnd = position + deleteCount;
        int remoteStart = remote.position;
        int remoteEnd = remote.position + remote.deleteCount;
        int remoteDelta = (remote.insertText == null ? 0 : remote.insertText.length()) - remote.deleteCount;

        boolean overlap = localStart < remoteEnd && remoteStart < localEnd;
        if (overlap) {
            return null;
        }

        int newPos = position;
        if (remoteStart < localStart) {
            newPos += remoteDelta;
        } else if (remoteStart == localStart && remoteDelta > 0) {
            newPos += remoteDelta;
        }

        if (newPos < 0) {
            return null;
        }
        return new OtTextOperation(newPos, deleteCount, insertText);
    }

    public static OtTextOperation between(String base, String target) {
        String left = base == null ? "" : base;
        String right = target == null ? "" : target;

        if (left.equals(right)) {
            return null;
        }

        int prefix = 0;
        int min = Math.min(left.length(), right.length());
        while (prefix < min && left.charAt(prefix) == right.charAt(prefix)) {
            prefix++;
        }

        int suffix = 0;
        while (suffix < left.length() - prefix && suffix < right.length() - prefix
                && left.charAt(left.length() - 1 - suffix) == right.charAt(right.length() - 1 - suffix)) {
            suffix++;
        }

        int deleteCount = left.length() - prefix - suffix;
        String insert = right.substring(prefix, right.length() - suffix);
        return new OtTextOperation(prefix, deleteCount, insert);
    }
}
