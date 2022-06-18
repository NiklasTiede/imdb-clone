import {makeStyles} from "@material-ui/core";
import React from "react";
import {Link, useMatch, useResolvedPath} from 'react-router-dom';


const useStyles = makeStyles({
    all: {
        backgroundColor: "#006dc7"
    },
    navbar: {
        fontSize: "15px",
        fontFamily: "ubuntu",
        position: "static"
    }
})

export default function Navbar() {
  return (
    <nav className="nav">
      <Link to="/" className="site-title">
        Site Name
      </Link>
      <ul>
        <CustomLink to="/pricing">Pricing</CustomLink>
        <CustomLink to="/about">About</CustomLink>
      </ul>
    </nav>
  )
}

type CustomLinkProps<T> = {
  to: string,
  children: any,
}

const CustomLink = <T extends string | number>(props: CustomLinkProps<T>) => {
  const resolvedPath = useResolvedPath(props.to)
  console.log(props.to)
  const isActive = useMatch({ path: resolvedPath.pathname, end: true })

  return (
    <li className={isActive ? "active" : ""}>
      <Link {...props}>
        {props.children}
      </Link>
    </li>
  )
}
