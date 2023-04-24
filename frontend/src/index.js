import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import "./index.css";
import App from "./App";
import NotFound from "./pages/NotFound";
import Novels from "./pages/Novels";
import NovelIntro from "./pages/NovelIntro";
import NovelRead from "./pages/NovelRead";
import NovelSearch from "./pages/NovelSearch";
import MyPage from "./pages/MyPage";
import NovelWrite from "./pages/NovelWrite";
import Landing from "./pages/Landing";
import OnlyLogin from "./components/login/OnlyLogin";
import Modal from "react-modal";
import { NovelContextProvider } from "./context/NovelContext";

const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    errorElement: <NotFound />,
    children: [
      { index: true, path: "/", element: <Landing /> },
      {
        path: "/login",
        element: <OnlyLogin />,
      },
      {
        path: "/library",
        element: <Novels />,
      },
      {
        path: "/library/:id/intro",
        element: <NovelIntro />,
      },
      {
        path: "/library/:id/read",
        element: <NovelRead />,
      },
      {
        path: "/library/search",
        element: <NovelSearch />,
      },
      {
        path: "/mypage",
        element: <MyPage />,
      },
      {
        path: "/laboratory",
        element: (
          <NovelContextProvider>
            <NovelWrite />
          </NovelContextProvider>
        ),
      },
    ],
  },
]);

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(
  // <React.StrictMode>

  <RouterProvider router={router} />

  // </React.StrictMode>
);

Modal.setAppElement("#root");
