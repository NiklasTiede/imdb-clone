import React, {useState, useEffect } from 'react';
import {makeStyles} from '@material-ui/core';

const useStyles = makeStyles({
  button: {
    margin: '30px',
    class: "btn btn-primary",
  },
  another: {

  }
})


export default function Counter() {
  const classes = useStyles()
  const [count, setCount] = useState(0);
  console.log(`My count is ${count}!`);
  const handleIncrement = () => setCount(currentCount => currentCount + 1);
  const handleDecrement = () => setCount(currentCount => currentCount - 1);
  useEffect(() => setCount(currentCount => currentCount + 1), []);

  return (
    <div>
      <h1 style={{margin: 10}} >{count}</h1>

      <button type="button" style={{margin: 10}} className={"btn btn-primary"} onClick={handleIncrement}>
        Increment
      </button>
      <button type="button" style={{margin: 3}} className={"btn btn-danger"} onClick={handleDecrement}>
        Decrement
      </button>
    </div>
  );

}
