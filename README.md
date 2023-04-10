# 멀티 클라이언트 서버 테스트

## 주요 이슈

#### 계속 nio selector에 read이벤트가 발생하여 클라이언트에서 종료하였음에도 계속 데이터가 들어오는 Half-Close 문제
> 서버에서 socketChannel.read(buffer)값이 -1을 받아 클라이언트쪽에서 연결 종료 메시지를 받았을 때 key.interestOps(0); // 관심 키 제거 를 호출하여 문제 해결

#### 고아 프로세스 생성 문제
> 발견하게 된 과정 : 자식 프로세스를 생성하지 않았음에도 생성한 것처럼 부모 프로세스가 동작을 함. -> 이상하게 여겨 자식 프로세스를 따로 
런해봤더니 사용중인 포트라고 나옴 -> 자식프로세스가 고아프로세스가 되어 혼자 돌고 있는 것 발견 

#### CLOSE_WAIT과 FIN_WAIT2 상태란 무엇이고 왜 발생하는가? + TIME_WAIT은?
> Active Close한 쪽이 FIN을 보내고 FIN_WAIT1상태로 대기 -> passive Close쪽이 CLOSE_WAIT으로 바꾸고 응답 ACK 전달
> -> ACK 받은 ACTIVE CLOSE는 FIN_WAIT2상태로 변경 -> passive close쪽은 종료 프로세스 진행후 FIN을 클라이언트에 보내 LAST_ACK 상태로 바꿈
> -> ACTIVE CLOSE쪽은 ACK를 PASSIVE CLOSE에 전송하고 TIME_WAIT으로 상태를 바꿈. TIME_WAIT에서 일정 시간이 지나면 CLOSE
> 
> 지금 코드의 경우엔 정상적으로 종료를 못하고 테스트 코드가 멈추질 않아 수동으로 프로세스 종료시 발생됨.

> TIME_WAIT이란? 패킷의 오동작을 막기 위함.
> 타임아웃은 60초로 설정되어 있음.
> 자바의 경우 Socket.SoLinger()같은 메소드로 시간을 할당할 수 있음.
> 
#### 왜 스레드로 테스트할땐 selector가 스레드들을 더 할당했는데 프로세스를 나누니 1개 스레드만 사용할까?
> 단일 스레드에서 동작하도록 설계되어 스레드에 할당될 경우 원치 않는 스레드를 추가 생성하고 동작이 이상해짐.

#### 서버에서 응답을 바로 바로 받아볼순 없나 논블로킹 같은 기법이면 inputstream read()의 블로킹 되는 문제를 막을 수 있지 않나?
> 1차 : 논블로킹(Readable)io인 ReadableByteChannel 클래스가 있으나 결국 channel에 읽기 작업을 하면 다른 스레드가 접근하지 못하도록 블로킹함. 
> 이러면 논블로킹을 쓰는 의미가 없다고 생각되어 사용하지 않았음. 또한, Selector와 같이 이벤트를 받아줄 수 있는 클래스가 존재하지 않음.
> 
> 마지막에 인풋스트림을 한 번에 읽어오는 건 효율면에서 좋지만, 그때그때 출력을 보고 싶을 땐 안좋다고 판단하여
> 일단 출력 전용 스레드를 하나 더 만들어서 계속 읽기 이벤트를 리스닝 하도록 만듦.(이게 폴링 방식?)

> 2차: nio.Pipe클래스를 이용한 전달
> 클라이언트를 Selector 기반 이벤트 기반으로 변경하여 Pipe로 프로세스간 통신을 하고 각 채널 이벤트를 등록한다음, SocketChannel의 이벤트를 감지하고 처리하게끔 변경
> 
> 기본적으로 논블로킹이지만 read와 write의 buffersize만큼은 버퍼가 걸리는 것 같다.
#### server disconnected가 정확히 요청 수(10번) 만큼 호출되고 연결을 받지 않았었는데 왜 1번만 호출 되는가?
> Thread.sleep()으로 실행때처럼 텀을 넣어주니 해결됨. 
>
> 발생한 이유는 응답을 받고 서버는 클라이언트 소켓을 연결 종료시키는 시간이 필요한데, 바로 부모프로세스에서 자식프로세스를 강제 종료 시켜 
> 출력이 종료되었거나 연결이 닫히는 걸 확인 못하고 그냥 나와버리는 듯

#### 클라이언트쪽에서 소켓 채널을 닫았는데 닫힌 채널에 write 이벤트가 발생하여 CancelledKeyException
> Writable과 Readable이 가능한 소켓 채널이 있는 것으로 추정, else if로 read 이벤트 타면 다시 못타게 막아놨더니 해결됨. 
> 근데 왜 write 이벤트가 소켓채널로 들어오는진 모르겠음. 

#### Client Disconnected를 출력하면 연결 종료를 하려했는데 왜 행업이 걸리는가..?
> 왜 행업되어 연결이 진행되다 그냥 죽어버리는가? 시간이 지나니 다 종료되고 established 된 연결 하나만 남는다.

> 해결 시도 : 혹시나 하나의 연결만 established가 된 게 신경 쓰여 Half-close 상태에 빠진 게 문제가 되나 싶어서 서버쪽에서도
> 드디어 알아낸 게 계속 sinkChannel로 read 이벤트가 발생하여 -1을 읽어온다!

> 최종 결론 : 설계부터 잘못되었다. Selector는 단일스레드를 타겟으로 설계된 모델이기 때문에 각각의 스레드 마다 독립적으로 open할 경우 원치 않는 동작이 수행되는 것이었다..

참고 자료: https://www.baeldung.com/java-nio-selector, https://homoefficio.github.io/2016/08/06/Java-NIO%EB%8A%94-%EC%83%9D%EA%B0%81%EB%A7%8C%ED%81%BC-non-blocking-%ED%95%98%EC%A7%80-%EC%95%8A%EB%8B%A4/,
https://tech.kakao.com/2016/04/21/closewait-timewait/