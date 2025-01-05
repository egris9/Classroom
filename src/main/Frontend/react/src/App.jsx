import React, { useState } from "react";
import SignIn from "./Templates/sign_in.jsx";
import SignUp from "./Templates/sign_up.jsx";
import Home from "./Templates/home.jsx";
import Creation from "./Templates/creation.jsx";
import JoinPage from "./Templates/join.jsx";



const App = () => {
  return (
    <>
        <SignIn/>
        <SignUp/>
        <Home />
        <Creation />
        <JoinPage />

    </>
  )
}

export default App;
