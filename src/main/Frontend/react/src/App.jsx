import SignIn from "./Templates/sign_in.jsx";
import SignUp from "./Templates/sign_up.jsx";
import Home from "./Templates/home.jsx";
import Creation from "./Templates/creation.jsx";
import JoinPage from "./Templates/join.jsx";
import Courses from "./Templates/courses.jsx";
import PDFUpload from "./Templates/PDFUpload.jsx"
import PDFDisplay from "./Templates/PDFDisplay.jsx"




const App = () => {
  return (
    <>
        <SignIn/>
        <SignUp/>
        <Home />
        <Creation />
        <JoinPage />
        <Courses />
        <PDFUpload />
        <PDFDisplay />

    </>
  )
}

export default App;
