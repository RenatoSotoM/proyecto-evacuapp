// src/services/sqlite-web-mock.ts
export function openDatabaseAsync() {
  return Promise.resolve({
    execAsync: () => Promise.resolve(),
    runAsync: () => Promise.resolve(),
    getFirstAsync: () => Promise.resolve(null),
    getAllAsync: () => Promise.resolve([]),
  });
}

export function openDatabaseSync() {
  return {
    execSync: () => {},
    runSync: () => {},
    getFirstSync: () => null,
    getAllSync: () => [],
  };
}