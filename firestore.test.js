const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write tasks or settings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("task_1")
      .set({
        userId: ALICE_UID,
        weekKey: "2026-W48",
        dayOfWeek: 1,
        title: "Yetkisiz gorev",
        category: "IS",
        priority: "NORMAL",
        completed: false,
        reminderEnabled: false,
        sortOrder: 0,
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").get());
});

test("Owner can create, list, toggle, update, and delete WeeklyTask with Kotlin payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const tasksCol = aliceDb.collection("users").doc(ALICE_UID).collection("tasks");

  await assertSucceeds(
    tasksCol.doc("task_1").set({
      userId: ALICE_UID,
      weekKey: "2026-W48",
      dayOfWeek: 1,
      title: "Haftalık toplantı notlarını hazırla",
      notes: "Sunum dosyalarını güncelle",
      category: "IS",
      priority: "YUKSEK",
      completed: false,
      reminderEnabled: true,
      reminderHour: 9,
      reminderMinute: 30,
      sortOrder: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List matches Kotlin repository observeTasks() query
  await assertSucceeds(tasksCol.where("weekKey", "==", "2026-W48").get());
  await assertSucceeds(tasksCol.get());

  // Partial toggle completion update
  await assertSucceeds(
    tasksCol.doc("task_1").update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Full task edit update preserving createdAt and userId
  await assertSucceeds(
    tasksCol.doc("task_1").update({
      title: "Haftalık toplantı notları tamamlandı",
      priority: "NORMAL",
      dayOfWeek: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(tasksCol.doc("task_1").delete());
});

test("Rejects invalid task payloads, extra @DocumentId id field, ghost fields, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const tasksCol = aliceDb.collection("users").doc(ALICE_UID).collection("tasks");

  // @DocumentId 'id' inside body must fail
  await assertFails(
    tasksCol.doc("task_bad_id").set({
      id: "task_bad_id",
      userId: ALICE_UID,
      weekKey: "2026-W48",
      dayOfWeek: 1,
      title: "Test",
      category: "IS",
      priority: "NORMAL",
      completed: false,
      reminderEnabled: false,
      sortOrder: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Invalid dayOfWeek (8) must fail
  await assertFails(
    tasksCol.doc("task_bad_day").set({
      userId: ALICE_UID,
      weekKey: "2026-W48",
      dayOfWeek: 8,
      title: "Test",
      category: "IS",
      priority: "NORMAL",
      completed: false,
      reminderEnabled: false,
      sortOrder: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Future timestamp must fail
  await assertFails(
    tasksCol.doc("task_future").set({
      userId: ALICE_UID,
      weekKey: "2026-W48",
      dayOfWeek: 1,
      title: "Test",
      category: "IS",
      priority: "NORMAL",
      completed: false,
      reminderEnabled: false,
      sortOrder: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  // Create valid doc then attempt immutable createdAt modification
  await assertSucceeds(
    tasksCol.doc("task_imm").set({
      userId: ALICE_UID,
      weekKey: "2026-W48",
      dayOfWeek: 3,
      title: "Sabah yürüyüşü",
      category: "SAGLIK",
      priority: "NORMAL",
      completed: false,
      reminderEnabled: false,
      sortOrder: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    tasksCol.doc("task_imm").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Cross-user isolation: Alice cannot read or write Bob's tasks or settings", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const now = new Date(Date.now() - 5000);
    await db.collection("users").doc(BOB_UID).collection("tasks").doc("bob_task").set({
      userId: BOB_UID,
      weekKey: "2026-W48",
      dayOfWeek: 1,
      title: "Bob task",
      category: "IS",
      priority: "NORMAL",
      completed: false,
      reminderEnabled: false,
      sortOrder: 0,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(BOB_UID).collection("settings").doc("preferences").set({
      userId: BOB_UID,
      morningHour: 7,
      morningMinute: 30,
      morningSummaryEnabled: true,
      mondayWeeklyBriefingEnabled: true,
      unlockBriefingEnabled: true,
      weekStartMonday: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("tasks").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("bob_task").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("bob_task").update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("settings").doc("preferences").get());
});

test("Owner can create and update PlanSettings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const settingsRef = aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences");

  await assertSucceeds(
    settingsRef.set({
      userId: ALICE_UID,
      morningHour: 7,
      morningMinute: 30,
      morningSummaryEnabled: true,
      mondayWeeklyBriefingEnabled: true,
      unlockBriefingEnabled: true,
      weekStartMonday: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    settingsRef.update({
      morningHour: 8,
      morningMinute: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
