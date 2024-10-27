package BiscuitBlitzer;

import java.util.ArrayList;
import java.util.List;

public class Achievements {
    private final List<Achievement> achievements;

    public Achievements() {
        achievements = new ArrayList<>();

        achievements.add(new Achievement("Master Blitzer", "Manually blitz 1,000 biscuits", 1000, false));
        achievements.add(new Achievement("Grinder", "Have the session open for a total of 24 hours", 86400, false));
        achievements.add(new Achievement("Event Horizon", "Trigger 10 events", 10, false));
        achievements.add(new Achievement("Flashbang", "Toggle dark mode 5 times in a second", 5, true));
        achievements.add(new Achievement("Personalizer", "Change the background color 25 times", 25, true));
    }

    public List<Achievement> getAchievementList() {
        return achievements;
    }

    public long getThreshold(String name) {
        for (Achievement achievement : achievements) {
            if (achievement.getName().equals(name)) {
                return achievement.getThreshold();
            }
        }

        return -1;
    }

    public boolean isLocked(String name) {
        for (Achievement achievement : achievements) {
            if (achievement.getName().equals(name)) {
                return achievement.isLocked();
            }
        }

        return false;
    }

    public void unlock(String name) {
        for (Achievement achievement : achievements) {
            if (achievement.getName().equals(name)) {
                achievement.unlock();
            }
        }
    }

    public static class Achievement {
        private final String name;
        private final String description;
        private final long threshold;
        private boolean isUnlocked = false;
        private final boolean isHidden;

        public Achievement(String name, String description, int threshold, boolean isHidden) {
            this.name = name;
            this.description = description;
            this.threshold = threshold;
            this.isHidden = isHidden;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public long getThreshold() { return threshold; }
        public boolean isLocked() { return !isUnlocked; }
        public void unlock() { isUnlocked = true; }
        public boolean isHidden() { return isHidden; }
    }
}
