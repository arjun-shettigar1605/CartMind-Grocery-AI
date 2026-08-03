import React, { useState, useEffect } from 'react';
import { noteService } from '../../services/noteService';

export const NotesManager = ({ token }) => {
  const [notes, setNotes] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [title, setTitle] = useState('');
  const [rawContent, setRawContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // Stage 2 State: Track converting note and parsed structured results
  const [convertingNoteId, setConvertingNoteId] = useState(null);
  const [structuredResults, setStructuredResults] = useState({});

  const loadNotes = async () => {
    const data = await noteService.getNotes(token);
    setNotes(data);
  };

  useEffect(() => {
    loadNotes();
  }, []);

  const handleSearch = async (e) => {
    const query = e.target.value;
    setSearchQuery(query);
    if (query.trim() === '') {
      loadNotes();
    } else {
      const results = await noteService.searchNotes(query, token);
      setNotes(results);
    }
  };

  const handleCreateNote = async (e) => {
    e.preventDefault();
    if (!title || !rawContent) return;

    setIsSubmitting(true);
    try {
      await noteService.createNote({ title, rawContent }, token);
      setTitle('');
      setRawContent('');
      await loadNotes();
    } catch (error) {
      console.error('Frontend log: Failed to create note.', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      await noteService.deleteNote(id, token);
      await loadNotes();
    } catch (error) {
      console.error('Frontend log: Failed to delete note.', error);
    }
  };

  // Stage 2 Handler: Trigger OpenAI Structured Output conversion
  const handleConvertNote = async (noteId) => {
    setConvertingNoteId(noteId);
    try {
      const structuredData = await noteService.convertNoteToCart(noteId, token);
      setStructuredResults(prev => ({ ...prev, [noteId]: structuredData }));
    } catch (error) {
      console.error('Frontend log: AI conversion error.', error);
      alert('Failed to convert note to structured grocery list. Check backend LLM configuration.');
    } finally {
      setConvertingNoteId(null);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      {/* Header & Offline Banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-primary dark:text-accent">Grocery Notes</h1>
          <p className="text-sm text-slate-600 dark:text-slate-400">
            Write your grocery needs in natural language for AI parsing.
          </p>
        </div>
        {!navigator.onLine && (
          <span className="bg-secondary text-white text-xs px-3 py-1 rounded-full">
            Offline Mode Active
          </span>
        )}
      </div>

      {/* Note Creation Form */}
      <form onSubmit={handleCreateNote} className="bg-white dark:bg-slate-800 p-4 rounded-xl shadow-md border border-slate-100 dark:border-slate-700 space-y-3">
        <input
          type="text"
          placeholder="Note Title (e.g., Weekend BBQ Party)"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="w-full p-2.5 rounded-lg border border-slate-300 dark:border-slate-600 bg-lightBg dark:bg-darkBg text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-accent"
          required
        />
        <textarea
          placeholder="Describe items in natural language... (e.g., Need 2kg chicken breast, 1 pack butter, butter chicken masala under ₹800)"
          value={rawContent}
          onChange={(e) => setRawContent(e.target.value)}
          rows={3}
          className="w-full p-2.5 rounded-lg border border-slate-300 dark:border-slate-600 bg-lightBg dark:bg-darkBg text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-accent"
          required
        />
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full md:w-auto px-6 py-2.5 bg-primary hover:bg-opacity-90 text-white font-medium rounded-lg shadow-sm transition duration-200"
        >
          {isSubmitting ? 'Saving...' : 'Save Grocery Note'}
        </button>
      </form>

      {/* Search Input */}
      <div className="relative">
        <input
          type="text"
          placeholder="Search notes..."
          value={searchQuery}
          onChange={handleSearch}
          className="w-full p-3 pl-10 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-200 shadow-sm focus:outline-none focus:ring-2 focus:ring-accent"
        />
        <svg className="w-5 h-5 absolute left-3 top-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </div>

      {/* Notes Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {notes.length === 0 ? (
          <p className="text-slate-500 dark:text-slate-400 col-span-2 text-center py-8">
            No grocery notes found. Create your first note above!
          </p>
        ) : (
          notes.map((note) => {
            const structuredData = structuredResults[note.id];

            return (
              <div 
                key={note.id} 
                className="bg-white dark:bg-slate-800 p-4 rounded-xl shadow-sm border border-slate-100 dark:border-slate-700 flex flex-col justify-between space-y-4"
              >
                <div>
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-semibold text-lg text-primary dark:text-accent">{note.title}</h3>
                    {note.isOffline && (
                      <span className="text-[10px] bg-secondary text-white px-2 py-0.5 rounded">
                        Unsynced
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-slate-700 dark:text-slate-300 whitespace-pre-wrap">
                    {note.rawContent}
                  </p>
                </div>

                {/* Stage 2 Render: Structured Output Display[cite: 1, 2] */}
                {structuredData && (
                  <div className="p-3 bg-lightBg dark:bg-darkBg rounded-lg border border-accent/30 space-y-2">
                    <div className="flex justify-between items-center border-b border-slate-200 dark:border-slate-700 pb-1">
                      <span className="text-xs font-bold text-accent uppercase">Structured List</span>
                      <span className="text-xs font-semibold text-primary dark:text-accent">
                        Est. Total: ₹{structuredData.totalEstimatedBudget}
                      </span>
                    </div>
                    <ul className="space-y-1 text-xs">
                      {structuredData.items?.map((item, idx) => (
                        <li key={idx} className="flex justify-between text-slate-600 dark:text-slate-300">
                          <span>{item.name} ({item.quantity})</span>
                          <span>₹{item.estimatedPrice}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Action Buttons */}
                <div className="flex justify-between items-center pt-2 border-t border-slate-100 dark:border-slate-700/50 text-xs text-slate-500 dark:text-slate-400">
                  <span>{new Date(note.createdAt).toLocaleDateString()}</span>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleConvertNote(note.id)}
                      disabled={convertingNoteId === note.id}
                      className="px-3 py-1 bg-accent hover:bg-opacity-90 text-white rounded font-medium transition"
                    >
                      {convertingNoteId === note.id ? 'Extracting...' : 'AI Convert'}
                    </button>
                    <button 
                      onClick={() => handleDelete(note.id)} 
                      className="text-red-500 hover:underline px-1 py-1"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};