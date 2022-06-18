import React, {useState} from 'react';

// lecture: callback function (state from child to parent)
export default function Greetings() {
  const [greeting, setGreeting] = useState('Hello Component!');

  const handleChange = (event: any) => setGreeting(event.target.value);

  return (
    <div>
      <Headline headline={greeting}/>
      <Input value={greeting} onChangeInput={handleChange}/>
    </div>);
}

const Headline = (props: any) => {
  return <h1>{props.headline}</h1>;
}

const Input = (props: any) => {
  return (
    <label>
      <input type="text"  value={props.value} onChange={props.onChangeInput} />
    </label>);
}



