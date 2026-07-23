const DATABASE_NAME = "wedding-ai-dress";
const DATABASE_VERSION = 1;
const STORE_NAME = "drafts";

const openDatabase = () =>
  new Promise((resolve, reject) => {
    const request = indexedDB.open(DATABASE_NAME, DATABASE_VERSION);

    request.onupgradeneeded = () => {
      const database = request.result;
      if (!database.objectStoreNames.contains(STORE_NAME)) {
        database.createObjectStore(STORE_NAME);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });

const withStore = async (mode, callback) => {
  if (typeof indexedDB === "undefined") return null;

  const database = await openDatabase();
  try {
    return await new Promise((resolve, reject) => {
      const transaction = database.transaction(STORE_NAME, mode);
      const store = transaction.objectStore(STORE_NAME);
      const request = callback(store);

      transaction.oncomplete = () => resolve(request?.result ?? null);
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error);
    });
  } finally {
    database.close();
  }
};

export const loadAiDressDraft = (key) =>
  withStore("readonly", (store) => store.get(key));

export const saveAiDressDraft = (key, draft) =>
  withStore("readwrite", (store) => store.put(draft, key));
