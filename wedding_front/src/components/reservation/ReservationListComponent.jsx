import { useEffect, useState } from "react";
import { getList } from "../../api/reservationApi";
import useCustomMove from "../../hooks/useCustomMove";
import PageComponent from "../common/PageComponent";

const initState = {
  dtoList: [],
  pageNumList: [],
  pageRequestDTO: null,
  prev: false,
  next: false,
  totalCount: 0,
  prevPage: 0,
  nextPage: 0,
  totalPage: 0,
  current: 0,
};

const ReservationListComponent = () => {
  const { page, size, moveToList, refresh, moveToRead } = useCustomMove();

  //serverData는 나중에 사용
  const [serverData, setServerData] = useState(initState);

  useEffect(() => {
    getList({ page, size }).then((data) => {
      console.log(data);
      setServerData(data);
    });
  }, [page, size, refresh]);

  return (
    <div className="border-2 border-blue-100 mt-10 mr-2 ml-2">
      <div className="flex flex-wrap mx-auto justify-center p-6">
        {serverData.dtoList.map((reservation) => (
          <div
            key={reservation.reservationId}
            className="w-full min-w-[400px]  p-2 m-2 rounded shadow-md"
            onClick={() => moveToRead(reservation.reservationId)}
          >
            <div className="flex ">
              <div className="font-extrabold text-2xl p-2 w-1/12">
                {reservation.reservationId}
              </div>
              <div className="text-1xl m-1 p-2 w-8/12 font-extrabold">
                {reservation.title}
              </div>
              <div className="text-1xl m-1 p-2 w-2/10 font-medium">
                {reservation.dueDate}
              </div>
            </div>
          </div>
        ))}
      </div>
      <PageComponent
        serverData={serverData}
        movePage={moveToList}
      ></PageComponent>
    </div>
  );
};

export default ReservationListComponent;
