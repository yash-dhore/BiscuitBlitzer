package BiscuitBlitzer;

import java.util.ArrayList;
import java.util.List;

public class Achievements {
    private final List<Achievement> achievements;

    public Achievements() {
        achievements = new ArrayList<>();

        achievements.add(new Achievement("Flashbang", "Toggle dark mode 5 times in a second", 5));
        achievements.add(new Achievement("Personalizer", "Change background color 25 times", 25));
        achievements.add(new Achievement("Master Blitzer", "Manually blitz 1,000 biscuits", 1000));
        achievements.add(new Achievement("Grinder", "Have the session open for a total of 24 hours", 86400));
    }

    public List<Achievement> getAchievements() {
        return achievements;
    }

    public void unlockAchievement(int index) {
        achievements.get(index).setUnlocked();
    }

    public static class Achievement {
        private final String name;
        private final String description;
        private final long threshold;
        private boolean isUnlocked = false;

        public Achievement(String name, String description, int threshold) {
            this.name = name;
            this.description = description;
            this.threshold = threshold;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isUnlocked() { return isUnlocked; }
        public void setUnlocked() { isUnlocked = true; }
        public long getThreshold() { return threshold; }
    }
}
