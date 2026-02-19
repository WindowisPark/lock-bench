# v2 Redis 튜닝 항목 (LOCK_TIMEOUT 개선)

## 목표
- LOCK_TIMEOUT 비율 감소
- 성공률 99% 이상 확보

## 튜닝 후보
1) 락 TTL 조정
   - 현재: 2s (코드 기준)
   - 목표: 작업 시간의 3~5배로 상향
2) 재시도/백오프 전략
   - 실패 시 지수 백오프 + 지터 적용
   - 최대 재시도 횟수 제한
3) 락 키 스코프
   - productId 기준 키 유지
   - 핫키 분산 전략(필요시)
4) Redis 연결 설정
   - timeout/connection pool 조정
5) Redis 서버 성능 점검
   - CPU, latency, slowlog 확인

## 적용 순서(권장)
1) TTL 상향
2) 재시도/백오프
3) Redis latency 점검
4) 핫키 분산 필요성 검토

## 검증 지표
- Success Rate >= 99%
- p95/p99 개선 여부
- LOCK_TIMEOUT 감소
