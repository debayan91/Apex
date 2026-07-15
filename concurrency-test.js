import http from 'k6/http';
import { check } from 'k6';

// 50 concurrent virtual users hitting the exact same endpoint simultaneously for 10 seconds.
export const options = {
  vus: 10,
  duration: '10s',
  thresholds: {
    http_req_failed: ['rate<0.01'], 
  },
};

export function setup() {
  const res = http.post('http://localhost:8081/user/create', JSON.stringify({
    username: 'concurrent_user_' + Date.now(),
    email: 'concurrent' + Date.now() + '@example.com',
    initialBalance: 0.00
  }), { headers: { 'Content-Type': 'application/json' } });

  let userId = 1;
  if (res.status === 200) {
      userId = JSON.parse(res.body).id;
  }
  return { userId: userId };
}

export default function (data) {
  const BASE_URL = 'http://localhost:8081';
  const userId = data.userId || 1;
  
  const depositPayload = JSON.stringify({ amount: 10.00 });
  const depositHeaders = { 'Content-Type': 'application/json' };
  
  const resDeposit = http.post(`${BASE_URL}/user/${userId}/deposit`, depositPayload, { headers: depositHeaders });
  
  check(resDeposit, {
    'deposit status is 200': (r) => r.status === 200,
  });
}
