import React from 'react';


interface NewNoteInputProps {
  addNote(note: string): void;
}


export default function Notes(props: any) {

  return (
    <div>
      <h1>Notes</h1>
      <input type="text" name="note" placeholder="Note" />
      <button>Add Note</button>
    </div>
  );
}
