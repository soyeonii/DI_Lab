# DI_Lab

한글 필기 인식 프로젝트

## Log

### - 2022.09.09 -
세선화 기능 구현 완료  
참고 : https://github.com/linbojin/Skeletonization-by-Zhang-Suen-Thinning-Algorithm

### - 2022.09.16 -
Todo : 획 데이터 추출

이미지 Numpy 배열 console 출력  
이미지에 프레임을 적용해 구간별 상위 point를 찾는 방식 적용  
일정 간격 이내에 다른 point가 있을 시 제외

### - 2022.09.19 -
최고 성능 프레임 크기, point 간격 색출

```py
# simplify()
size = 2
space = 3
```

### - 2022.09.25 -
획 데이터 추출 기능 구현 ing...

앞서 찾은 point를 하나씩 확인하며 주변의 기울기가 같은 point들을 스택에 담고 기울기를 저장한다.   
전과 비교해 기울기가 달라지면 스택을 비우고 새로 시작한다.

```py
# devide()
size = 8
```

### - 2022.11.28 -
case1 데이터 추가, 테스트 완료(성능 향상)   
획 데이터 분리 기능 일부 수정   
