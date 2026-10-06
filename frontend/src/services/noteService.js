import axios from 'axios';

const API_URL = 'http://localhost:8080/api/notes';
const OFFLINE_KEY = 'cartmind_offline_notes';

// Helper to handle offline storage
const getOfflineNotes = () => JSON.parse(localStorage.getItem(OFFLINE_KEY) || '[]');
const setOfflineNotes = (notes) => localStorage.setItem(OFFLINE_KEY, JSON.stringify(notes));

export const noteService = {
  // Fetch notes with offline fallback
  getNotes: async (token) => {
    if (!navigator.onLine) {
      console.warn('Frontend log: Offline mode active. Serving notes from local storage.');
      return getOfflineNotes();
    }

    try {
      const response = await axios.get(API_URL, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setOfflineNotes(response.data); // Update offline cache
      return response.data;
    } catch (error) {
      console.error('Frontend log: API failure fetching notes. Falling back to offline cache.', error);
      return getOfflineNotes();
    }
  },

  // Create note with offline support
  createNote: async (noteData, token) => {
    if (!navigator.onLine) {
      console.warn('Frontend log: Offline mode active. Saving note locally.');
      const localNotes = getOfflineNotes();
      const tempNote = { ...noteData, id: Date.now(), isOffline: true, createdAt: new Date().toISOString() };
      const updated = [tempNote, ...localNotes];
      setOfflineNotes(updated);
      return tempNote;
    }

    const response = await axios.post(API_URL, noteData, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  },

  // Search notes
  searchNotes: async (query, token) => {
    if (!navigator.onLine) {
      const localNotes = getOfflineNotes();
      return localNotes.filter(n => 
        n.title.toLowerCase().includes(query.toLowerCase()) || 
        n.rawContent.toLowerCase().includes(query.toLowerCase())
      );
    }

    const response = await axios.get(`${API_URL}/search?q=${encodeURIComponent(query)}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  },

  // Delete note
  deleteNote: async (id, token) => {
    if (!navigator.onLine) {
      const localNotes = getOfflineNotes().filter(n => n.id !== id);
      setOfflineNotes(localNotes);
      return;
    }

    await axios.delete(`${API_URL}/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
  },

  convertNoteToCart: async (noteId, token) => {
    try {
      const response = await axios.post(`${API_URL}/${noteId}/convert`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      return response.data;
    } catch (error) {
      console.error('Frontend log: API failure during LLM conversion.', error);
      throw error;
    }
  }
};