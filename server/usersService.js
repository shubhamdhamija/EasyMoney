// Simple users service for demo. Replace with your DB-backed users.

const users = [
  { id: 'user1', deviceToken: '', email: 'alice@example.com' },
  { id: 'user2', deviceToken: '', email: 'bob@example.com' }
];

async function getAllUsers() {
  // In production, query your user DB
  return users;
}

async function getUserById(id) {
  return users.find(u => u.id === id);
}

// For event engine: get all users watching a symbol
async function getUsersWatching(symbol) {
  // In production, query your DB (watchlist join users)
  // Here, return all users for demo
  return users;
}

module.exports = { getAllUsers, getUserById, getUsersWatching };
