package com.cherglow.gcs.model;

/**
 * 航点实体（B1/B2）：在经纬度基础上扩展高度、停留时间、动作指令与优先级。
 * 地图仅绘制坐标；规划面板可编辑全部属性，冲突检测复用 {@link #distanceTo}。
 */
public final class Waypoint {

    /** 航点动作指令（任务书点名：巡航/悬停/拍照；区别于固件飞行模式）。 */
    public enum Action {
        CRUISE("巡航"), HOLD("悬停"), PHOTO("拍照");

        private final String label;

        Action(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** 按枚举名或中文标签反查；未知返回 null。 */
        public static Action from(String s) {
            if (s == null) {
                return null;
            }
            for (Action a : values()) {
                if (a.name().equalsIgnoreCase(s) || a.label.equals(s)) {
                    return a;
                }
            }
            return null;
        }
    }

    /** 航点角色：起点 / 终点 / 普通航点。用于路径规划模式。 */
    public enum Role {
        WAYPOINT("航点"), START("起点"), END("终点");

        private final String label;

        Role(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** 按枚举名或中文标签反查；未知返回 null。 */
        public static Role from(String s) {
            if (s == null) {
                return null;
            }
            for (Role r : values()) {
                if (r.name().equalsIgnoreCase(s) || r.label.equals(s)) {
                    return r;
                }
            }
            return null;
        }
    }

    private final int id;
    private double lat;
    private double lon;
    private double altM = 0;
    private int staySec = 0;
    private Action action = Action.CRUISE;
    private int priority = 0;
    private Role role = Role.WAYPOINT;

    public Waypoint(int id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }

    public int getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getAltM() {
        return altM;
    }

    public void setAltM(double altM) {
        this.altM = altM;
    }

    public int getStaySec() {
        return staySec;
    }

    public void setStaySec(int staySec) {
        this.staySec = staySec;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    /** 两点间球面距离（米），haversine 公式。 */
    public double distanceTo(Waypoint o) {
        if (o == null) {
            return 0;
        }
        return haversineM(lat, lon, o.lat, o.lon);
    }

    public static double haversineM(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.sqrt(h));
    }
}