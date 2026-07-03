import { useEffect, useState } from "react";
import { getOne } from "../../api/reservationApi";
import useCustomMove from "../../hooks/useCustomMove";

const initState = {
  reservationId: 0,
  title: "",
  writer: "",
  dueDate: null,
  complete: false,
};

const ReadComponent = ({ reservationId }) => {
  const [reservation, setReservation] = useState(initState); //아직 reservation는 사용하지 않음

  const { moveToList, moveToModify } = useCustomMove();

  useEffect(() => {
    getOne(reservationId).then((data) => {
      console.log(data);
      setReservation(data);
    });
  }, [reservationId]);

  return (
    <div className="border-2 border-sky-200 mt-10 m-2 p-4 ">
      {makeDiv("ReservationId", reservation.reservationId)}
      {makeDiv("Writer", reservation.writer)}
      {makeDiv("Title", reservation.title)}
      {makeDiv("Due Date", reservation.dueDate)}
      {makeDiv("Complete", reservation.complete ? "Completed" : "Not Yet")}

      {/* buttons.........start */}
      <div className="flex justify-end p-4">
        <button
          type="button"
          className="rounded p-4 m-2 text-xl w-32 text-white bg-blue-500"
          onClick={() => moveToList()}
        >
          List
        </button>
        <button
          type="button"
          className="rounded p-4 m-2 text-xl w-32 text-white bg-red-500"
          onClick={() => moveToModify(reservationId)}
        >
          Modify
        </button>
      </div>
    </div>
  );
};

const makeDiv = (title, value) => (
  <div className="flex justify-center">
    <div className="relative mb-4 flex w-full flex-wrap items-stretch">
      <div className="w-1/5 p-6 text-right font-bold">{title}</div>
      <div className="w-4/5 p-6 rounded-r border border-solid shadow-md">
        {value}
      </div>
    </div>
  </div>
);

export default ReadComponent;
