import React, { useRef, useState } from "react";
import style from "./mynovel.module.css";
import BookList from "./BookList";
import Gototop from "../common/GoToTop";

export default function MyNovel() {
  const parentRef = useRef<HTMLDivElement | null>(null);
  const childRef = useRef<HTMLDivElement | null>(null);
  const parentRef2 = useRef<HTMLDivElement | null>(null);
  const childRef2 = useRef<HTMLDivElement | null>(null);

  const [isCollapse, setIsCollapse] = React.useState(false);

  const handleButtonClick = React.useCallback(
    (e: React.MouseEvent<HTMLDivElement, MouseEvent>) => {
      const icon = document.querySelector("#tri1");
      e.stopPropagation();
      if (parentRef.current === null || childRef.current === null) {
        return;
      }
      if (parentRef.current.clientHeight > 0) {
        icon?.setAttribute("style", "transform: rotate(0deg)");
        parentRef.current.style.height = "0";
      } else {
        parentRef.current.style.height = `${childRef.current.clientHeight}px`;
        icon?.setAttribute("style", "transform: rotate(180deg)");
      }
      setIsCollapse(!isCollapse);
    },
    [isCollapse]
  );

  const handleButtonClick2 = React.useCallback(
    (e: React.MouseEvent<HTMLDivElement, MouseEvent>) => {
      const icon = document.querySelector("#tri2");
      e.stopPropagation();
      if (parentRef2.current === null || childRef2.current === null) {
        return;
      }
      if (parentRef2.current.clientHeight > 0) {
        icon?.setAttribute("style", "transform: rotate(0deg)");
        parentRef2.current.style.height = "0";
      } else {
        parentRef2.current.style.height = `${childRef2.current.clientHeight}px`;
        icon?.setAttribute("style", "transform: rotate(180deg)");
      }
      setIsCollapse(!isCollapse);
    },
    [isCollapse]
  );

  const [mylen, setMylen] = useState(0);
  const [likelen, setLikelen] = useState(0);

  const book = {
    id: 999,
    cover_img: "",
    introduction: "",
    novel_stats: { hit_count: 10, like_count: 10, comment_count: 10 },
    author: "",
    title: "",
  };
  const books = [book, book, book, book, book, book, book, book];

  return (
    <div className={style.container}>
      <hr className={style.line} />

      <div className={style.titlebar} onClick={handleButtonClick}>
        <div className={style.mynoveltitle}>
          <span className={style.step}>1</span>
          <img
            src={process.env.PUBLIC_URL + "/icon/logo.svg"}
            className={style.logo}
            alt="logo"
          />
        </div>

        <div className={style.title}>제작한 소설</div>

        <button>{mylen}</button>
        <div className={style.triangle}>
          <img
            src={process.env.PUBLIC_URL + "/icon/triangle.svg"}
            alt=""
            id="tri1"
          />
        </div>
      </div>

      <div ref={parentRef} className={style.booklist} id="booklist1">
        <div ref={childRef} style={{ width: "100%" }}>
          <BookList books={books} />
        </div>
      </div>

      <hr className={style.line} />

      <div className={style.titlebar} onClick={handleButtonClick2}>
        <div className={style.mynoveltitle}>
          <span className={style.step}>2</span>
          <img
            src={process.env.PUBLIC_URL + "/icon/logo.svg"}
            className={style.logo}
            alt="logo"
          />
        </div>

        <div className={style.title}>좋아요 누른 소설</div>

        <button>{likelen}</button>
        <div className={style.triangle}>
          <img
            src={process.env.PUBLIC_URL + "/icon/triangle.svg"}
            alt=""
            id="tri2"
          />
        </div>
      </div>

      <div ref={parentRef2} className={style.booklist} id="booklist2">
        <div ref={childRef2} style={{ width: "100%" }}>
          <BookList books={books} />
        </div>
      </div>

      <Gototop />
    </div>
  );
}
