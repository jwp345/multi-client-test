# 멀티 클라이언트 서버 테스트

### 고아 프로세스 생성 문제
발견하게 된 과정 : 자식 프로세스를 생성하지 않았음에도 생성한 것처럼 부모 프로세스가 동작을 함. -> 이상하게 여겨 자식 프로세스를 따로 
런해봤더니 사용중인 포트라고 나옴 -> 자식프로세스가 고아프로세스가 되어 혼자 돌고 있는 것 발견 

### CLOSE_WAIT과 FIN_WAIT2 상태란 무엇이고 왜 발생하는가?