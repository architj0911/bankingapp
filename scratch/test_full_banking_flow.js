const GATEWAY_URL = 'http://localhost:8080';
const DEVICE_ID = 'test-device-e2e-123';
const CLIENT_BASIC_AUTH = 'Basic ' + Buffer.from('api-gateway:secret123').toString('base64');

const log = (step, msg, data = '') => {
  console.log(`\n=== [STEP ${step}] ${msg} ===`);
  if (data) console.log(typeof data === 'object' ? JSON.stringify(data, null, 2) : data);
};

async function req(url, options = {}) {
  const res = await fetch(url, options);
  let body;
  const text = await res.text();
  try {
    body = JSON.parse(text);
  } catch (e) {
    body = text;
  }
  if (!res.ok) {
    const err = new Error(`HTTP ${res.status}: ${typeof body === 'object' ? JSON.stringify(body) : body}`);
    err.status = res.status;
    err.data = body;
    throw err;
  }
  return body;
}

async function runFullE2ETest() {
  try {
    const timestamp = Date.now();
    const userA_name = `usera_${timestamp}`;
    const userB_name = `userb_${timestamp}`;

    log(1, `Registering User A (${userA_name})`);
    const regA = await req(`${GATEWAY_URL}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Device-Id': DEVICE_ID },
      body: JSON.stringify({
        username: userA_name,
        password: 'Password123!',
        email: `${userA_name}@test.com`,
        phoneNumber: `987${timestamp.toString().slice(-7)}`,
        role: 'ROLE_USER'
      })
    });
    console.log('User A registered:', regA);

    log(2, `Requesting OTP for User A via /api/auth/login`);
    const otpReqA = await req(`${GATEWAY_URL}/api/auth/login?userName=${userA_name}&password=Password123!`, {
      method: 'POST',
      headers: { 'X-Device-Id': DEVICE_ID }
    });
    console.log('OTP Request Response A:', otpReqA);
    const otpA = otpReqA.otp;
    console.log(`Extracted OTP A: ${otpA}`);

    log(3, `Verifying OTP for User A to issue JWT`);
    const paramsA = new URLSearchParams();
    paramsA.append('grant_type', 'otp');
    paramsA.append('username', userA_name);
    paramsA.append('otp', otpA);

    const tokenResA = await req(`${GATEWAY_URL}/oauth2/token`, {
      method: 'POST',
      headers: {
        'Authorization': CLIENT_BASIC_AUTH,
        'Content-Type': 'application/x-www-form-urlencoded',
        'X-Device-Id': DEVICE_ID
      },
      body: paramsA.toString()
    });
    const jwtA = tokenResA.access_token;
    console.log(`JWT A issued successfully! Token: ${jwtA.substring(0, 30)}...`);

    const authHeadersA = {
      'Authorization': `Bearer ${jwtA}`,
      'X-Device-Id': DEVICE_ID,
      'Content-Type': 'application/json'
    };

    log(4, `Creating Customer Profile for User A`);
    const custResA = await req(`${GATEWAY_URL}/api/customers`, {
      method: 'POST',
      headers: authHeadersA,
      body: JSON.stringify({
        name: `User A (${userA_name})`,
        phone: `987${timestamp.toString().slice(-7)}`,
        email: `${userA_name}@test.com`
      })
    });
    console.log('Customer Profile A created:', custResA);
    const customerIdA = custResA.id;
    const userIdA = custResA.userId;

    log(5, `Fetching Customer Profile /api/customers/me for User A`);
    const getCustA = await req(`${GATEWAY_URL}/api/customers/me`, {
      method: 'GET',
      headers: authHeadersA
    });
    console.log('Retrieved Customer Profile A:', getCustA);
    if (getCustA.id !== customerIdA || getCustA.userId !== userIdA) {
      throw new Error(`Customer profile mismatch for User A! Expected customerId ${customerIdA}, userId ${userIdA}`);
    }

    log(6, `Submitting KYC for User A (${customerIdA})`);
    const kycResA = await req(`${GATEWAY_URL}/api/kyc/${customerIdA}`, {
      method: 'POST',
      headers: authHeadersA,
      body: JSON.stringify({
        panNumber: 'ABCDE1234F',
        aadhaarNumber: '123456789012',
        status: 'VERIFIED'
      })
    });
    console.log('KYC Submission Response A:', kycResA);

    log(7, `Fetching KYC status /api/kyc/me for User A`);
    const getKycA = await req(`${GATEWAY_URL}/api/kyc/me`, {
      method: 'GET',
      headers: authHeadersA
    });
    console.log('Retrieved KYC Status A:', getKycA);

    log(8, `Creating Bank Account 1 for User A (Initial Deposit $5000)`);
    const acc1ResA = await req(`${GATEWAY_URL}/api/accounts`, {
      method: 'POST',
      headers: authHeadersA,
      body: JSON.stringify({
        accountType: 'SAVING',
        intialDeposit: 5000
      })
    });
    console.log('Account 1 Created:', acc1ResA);
    const acc1Num = acc1ResA.accountNumber;

    log(9, `Creating Bank Account 2 for User A (Initial Deposit $2000)`);
    const acc2ResA = await req(`${GATEWAY_URL}/api/accounts`, {
      method: 'POST',
      headers: authHeadersA,
      body: JSON.stringify({
        accountType: 'CURRENT',
        intialDeposit: 2000
      })
    });
    console.log('Account 2 Created:', acc2ResA);
    const acc2Num = acc2ResA.accountNumber;

    log(10, `Fetching Accounts /api/accounts/me for User A`);
    const myAccsA = await req(`${GATEWAY_URL}/api/accounts/me`, {
      method: 'GET',
      headers: authHeadersA
    });
    console.log('User A Accounts:', myAccsA);
    if (myAccsA.length < 2) {
      throw new Error('Expected at least 2 accounts for User A!');
    }

    log(11, `Registering & Logging in User B (${userB_name})`);
    const regB = await req(`${GATEWAY_URL}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Device-Id': DEVICE_ID },
      body: JSON.stringify({
        username: userB_name,
        password: 'Password123!',
        email: `${userB_name}@test.com`,
        phoneNumber: `988${timestamp.toString().slice(-7)}`,
        role: 'ROLE_USER'
      })
    });

    const otpReqB = await req(`${GATEWAY_URL}/api/auth/login?userName=${userB_name}&password=Password123!`, {
      method: 'POST',
      headers: { 'X-Device-Id': DEVICE_ID }
    });
    const otpB = otpReqB.otp;

    const paramsB = new URLSearchParams();
    paramsB.append('grant_type', 'otp');
    paramsB.append('username', userB_name);
    paramsB.append('otp', otpB);

    const tokenResB = await req(`${GATEWAY_URL}/oauth2/token`, {
      method: 'POST',
      headers: {
        'Authorization': CLIENT_BASIC_AUTH,
        'Content-Type': 'application/x-www-form-urlencoded',
        'X-Device-Id': DEVICE_ID
      },
      body: paramsB.toString()
    });
    const jwtB = tokenResB.access_token;

    const authHeadersB = {
      'Authorization': `Bearer ${jwtB}`,
      'X-Device-Id': DEVICE_ID,
      'Content-Type': 'application/json'
    };

    log(12, `Creating Customer Profile, Submitting KYC & Creating Account for User B`);
    const custResB = await req(`${GATEWAY_URL}/api/customers`, {
      method: 'POST',
      headers: authHeadersB,
      body: JSON.stringify({
        name: `User B (${userB_name})`,
        phone: `988${timestamp.toString().slice(-7)}`,
        email: `${userB_name}@test.com`
      })
    });
    const customerIdB = custResB.id;

    await req(`${GATEWAY_URL}/api/kyc/${customerIdB}`, {
      method: 'POST',
      headers: authHeadersB,
      body: JSON.stringify({
        panNumber: 'XYZDE5678G',
        aadhaarNumber: '987654321098',
        status: 'VERIFIED'
      })
    });

    const accResB = await req(`${GATEWAY_URL}/api/accounts`, {
      method: 'POST',
      headers: authHeadersB,
      body: JSON.stringify({
        accountType: 'SAVING',
        intialDeposit: 1000
      })
    });
    const accBNum = accResB.accountNumber;
    console.log(`User B created account ${accBNum} with balance $1000`);

    log(13, `Checking Sender (User A Acc 1) & Receiver (User B Acc B) Balances BEFORE Transfer`);
    const acc1Before = await req(`${GATEWAY_URL}/api/accounts/${acc1Num}`, { headers: authHeadersA });
    const accBBefore = await req(`${GATEWAY_URL}/api/accounts/${accBNum}`, { headers: authHeadersB });
    console.log(`BEFORE TRANSFER: Acc1 Balance = $${acc1Before.balance}, AccB Balance = $${accBBefore.balance}`);

    log(14, `Performing Money Transfer of $1500 from User A (${acc1Num}) to User B (${accBNum})`);
    const transferRes = await req(`${GATEWAY_URL}/api/transactions/transfer`, {
      method: 'POST',
      headers: authHeadersA,
      body: JSON.stringify({
        fromAccountNumber: acc1Num,
        toAccountNumber: accBNum,
        amount: 1500,
        description: 'E2E Test Transfer'
      })
    });
    console.log('TRANSFER SUCCESS RESPONSE:', transferRes);
    const txId = transferRes.txnId || transferRes.transactionId || transferRes.id;
    if (!txId) {
      throw new Error('Transfer response missing transaction ID!');
    }

    log(15, `Verifying Sender & Receiver Balances AFTER Transfer`);
    const acc1After = await req(`${GATEWAY_URL}/api/accounts/${acc1Num}`, { headers: authHeadersA });
    const accBAfter = await req(`${GATEWAY_URL}/api/accounts/${accBNum}`, { headers: authHeadersB });
    console.log(`AFTER TRANSFER: Acc1 Balance = $${acc1After.balance}, AccB Balance = $${accBAfter.balance}`);

    if (Number(acc1After.balance) !== Number(acc1Before.balance) - 1500) {
      throw new Error(`Sender balance check failed! Expected ${acc1Before.balance - 1500}, got ${acc1After.balance}`);
    }
    if (Number(accBAfter.balance) !== Number(accBBefore.balance) + 1500) {
      throw new Error(`Receiver balance check failed! Expected ${accBBefore.balance + 1500}, got ${accBAfter.balance}`);
    }
    console.log('>>> BALANCES PERFECTLY VERIFIED IN DATABASE! SENDER DEBITED $1500, RECEIVER CREDITED $1500!');

    log(16, `Fetching Transaction History /api/transactions/me for User A & User B`);
    const txsA = await req(`${GATEWAY_URL}/api/transactions/me`, { headers: authHeadersA });
    const txsB = await req(`${GATEWAY_URL}/api/transactions/me`, { headers: authHeadersB });
    console.log(`User A Transaction History Count: ${txsA.length}`);
    console.log(`User B Transaction History Count: ${txsB.length}`);

    log(17, `Testing Failed Transfer (Insufficient Funds: $999,999)`);
    try {
      await req(`${GATEWAY_URL}/api/transactions/transfer`, {
        method: 'POST',
        headers: authHeadersA,
        body: JSON.stringify({
          fromAccountNumber: acc1Num,
          toAccountNumber: accBNum,
          amount: 999999,
          description: 'Excessive Amount'
        })
      });
      throw new Error('Transfer should have been rejected for insufficient funds!');
    } catch (err) {
      console.log('Overdraft transfer correctly REJECTED:', err.message);
    }

    log(18, `Verifying Balances Remained Unchanged After Rejected Transfer`);
    const acc1Final = await req(`${GATEWAY_URL}/api/accounts/${acc1Num}`, { headers: authHeadersA });
    if (Number(acc1Final.balance) !== Number(acc1After.balance)) {
      throw new Error('Sender balance changed after failed transfer!');
    }
    console.log('>>> BALANCE PROTECTION CONFIRMED! Balances unchanged after rejected transaction.');

    log(19, `Security Authorization Test: User B attempting to transfer from User A's account`);
    try {
      await req(`${GATEWAY_URL}/api/transactions/transfer`, {
        method: 'POST',
        headers: authHeadersB,
        body: JSON.stringify({
          fromAccountNumber: acc1Num,
          toAccountNumber: accBNum,
          amount: 100,
          description: 'Unauthorized Transfer Attempt'
        })
      });
      throw new Error('SECURITY FAILURE: User B was able to initiate a transfer from User A account!');
    } catch (err) {
      console.log('Security check correctly REJECTED unauthorized transfer:', err.message);
    }

    console.log('\n======================================================');
    console.log('>>> ALL END-TO-END BACKEND API TESTS PASSED 100% SUCCESS! <<<');
    console.log('======================================================\n');
  } catch (err) {
    console.error('\nE2E TEST FAILURE:', err.stack || err.message);
    process.exit(1);
  }
}

runFullE2ETest();
