const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-no-project",
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("unauthenticated user cannot read cards", async () => {
  const unauthed = testEnv.unauthenticatedContext();
  await assertFails(unauthed.firestore().collection("users/user1/cards").get());
});

test("authenticated user can create their own card", async () => {
  const authed = testEnv.authenticatedContext("user1");
  await assertSucceeds(authed.firestore().collection("users/user1/cards").add({
    userId: "user1",
    name: "Alex Rivera",
    company: "Apex Tech",
    title: "CTO",
    phone: "555-0199",
    email: "alex@apextech.io",
    isFavorite: true,
    tags: ["tech", "startup"]
  }));
});

test("authenticated user cannot access other user's cards", async () => {
  const authedUser2 = testEnv.authenticatedContext("user2");
  await assertFails(authedUser2.firestore().collection("users/user1/cards").get());
  await assertFails(authedUser2.firestore().collection("users/user1/cards").doc("card1").set({
    userId: "user1",
    name: "Fake"
  }));
});
