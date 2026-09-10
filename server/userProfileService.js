/**
 * User profile service - manages user settings and preferences
 */
const userProfiles = {};

module.exports = {
  getProfile(userId) {
    if (!userProfiles[userId]) {
      userProfiles[userId] = {
        userId,
        riskProfile: 'medium',
        watchlist: [],
        searchHistory: [],
        preferences: {}
      };
    }
    return userProfiles[userId];
  },

  updateProfile(userId, updates) {
    const profile = this.getProfile(userId);
    Object.assign(profile, updates);
    return profile;
  },

  recordSearch(userId, symbol) {
    const profile = this.getProfile(userId);
    if (!profile.searchHistory) profile.searchHistory = [];
    if (!profile.searchHistory.includes(symbol)) {
      profile.searchHistory.unshift(symbol);
      if (profile.searchHistory.length > 50) {
        profile.searchHistory.pop();
      }
    }
  }
};

