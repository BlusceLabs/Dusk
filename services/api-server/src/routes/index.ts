import { Router, type IRouter } from "express";
import healthRouter from "./health.js";
import mediaRouter from "./media.js";
import socialRouter from "./social.js";
import streamsRouter from "./streams.js";
import eventsRouter from "./events.js";
import emailRouter from "./email.js";
import paymentsRouter from "./payments.js";
import pushRouter from "./push.js";

const router: IRouter = Router();

router.use(healthRouter);
router.use("/media", mediaRouter);
router.use("/social", socialRouter);
router.use("/streams", streamsRouter);
router.use("/events", eventsRouter);
router.use("/email", emailRouter);
router.use("/payments", paymentsRouter);
router.use("/push", pushRouter);

export default router;
