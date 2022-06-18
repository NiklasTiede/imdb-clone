import React, {useState} from 'react';
import {makeStyles} from '@material-ui/core';

const useStyles = makeStyles({
  login: {
    margin: '30px',
  },
  button: {
    margin: '10px'
  }
})

export default function Counters() {
  const counterStates = [
    {id: 1, value: 4},
    {id: 2, value: 2},
    {id: 3, value: 6},
  ];

  const handleDelete = (counterId: number) => {
    const filtCounters = counters.filter((counter) => counter.id !== counterId)
    setCounters(filtCounters);
  };

  const handleReset = () => {
    const resetCounters = counters.map(c => {c.value = 0; return c});
    console.log(resetCounters);
    setCounters(resetCounters);
  };

  const handleIncrement = (counter: any) => {
    const incrementedCounters = [...counters];
    const index = incrementedCounters.indexOf(counter);
    incrementedCounters[index] = {...counter};
    console.log(incrementedCounters);
    incrementedCounters[index].value++;
    setCounters(incrementedCounters);
    console.log(incrementedCounters);
  };

  // parametrize function (arrow function)
  const wrap = (type: string, str: string) => `<${type}>${str}</${type}>`
  // update objects (spread) spread operator does a shallow copy
  const person = { name: "John" };
  const updated = {...person, name: "Jack"};

  const [counters, setCounters] = useState(counterStates);

  return (
    <div>
      <button
        onClick={handleReset}
        className={"btn btn-primary btn-sm m-2"}
      >Reset</button>
      <h1>Counters</h1>
      {counters.map(counter => (
        <SingleCounter
          onIncrement={handleIncrement}
          key={counter.id}
          onDelete={handleDelete}
          value={counter.value}
          id={counter.id}
        />
      ))}
    </div>
  );
}

function SingleCounter(props: any) {
  // const [counter, setCounter] = useState(props.value);
  const classes = useStyles()

  return (
    <div className={classes.login}>
      <h4>Counter #{props.id}</h4>
      <span style={ {fontSize: '30px'} }  className={classes.button} >{props.value}</span>
      <button
        onClick={() => props.onIncrement(props.id) }
        className="btn btn-warning btn-sm" >
        Increment
      </button>

      <button
        onClick={ () => props.onDelete(props.id) }
        className="btn btn-danger btn-sm m-2" >
        Delete
      </button>

    </div>
  );

}



