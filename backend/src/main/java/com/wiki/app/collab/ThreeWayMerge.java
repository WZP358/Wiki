package com.wiki.app.collab;

public final class ThreeWayMerge {
    private ThreeWayMerge() {
    }

    public static String merge(String base, String server, String client) {
        base = base == null ? "" : base;
        server = server == null ? "" : server;
        client = client == null ? "" : client;

        if (server.equals(client)) {
            return server;
        }
        if (base.equals(server)) {
            return client;
        }
        if (base.equals(client)) {
            return server;
        }

        Change serverChange = diffRange(base, server);
        Change clientChange = diffRange(base, client);

        if (serverChange == null || clientChange == null) {
            return null;
        }

        if (serverChange.end <= clientChange.start) {
            return base.substring(0, serverChange.start)
                    + serverChange.replacement
                    + base.substring(serverChange.end, clientChange.start)
                    + clientChange.replacement
                    + base.substring(clientChange.end);
        }

        if (clientChange.end <= serverChange.start) {
            return base.substring(0, clientChange.start)
                    + clientChange.replacement
                    + base.substring(clientChange.end, serverChange.start)
                    + serverChange.replacement
                    + base.substring(serverChange.end);
        }

        return null;
    }

    private static Change diffRange(String base, String target) {
        int prefix = commonPrefix(base, target);
        int suffix = commonSuffix(base, target, prefix);

        int baseEnd = base.length() - suffix;
        int targetEnd = target.length() - suffix;

        if (prefix > baseEnd || prefix > targetEnd) {
            return null;
        }

        return new Change(prefix, baseEnd, target.substring(prefix, targetEnd));
    }

    private static int commonPrefix(String a, String b) {
        int len = Math.min(a.length(), b.length());
        int i = 0;
        while (i < len && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    private static int commonSuffix(String a, String b, int prefix) {
        int i = 0;
        int max = Math.min(a.length(), b.length()) - prefix;
        while (i < max && a.charAt(a.length() - 1 - i) == b.charAt(b.length() - 1 - i)) {
            i++;
        }
        return i;
    }

    private record Change(int start, int end, String replacement) {
    }
}
