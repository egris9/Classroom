import { useCallback, useEffect, useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import ChatPanel from "@/components/ChatPanel";
import PageContainer from "@/components/PageContainer";
import SignUpGate from "@/components/SignUpGate";
import ToolsExercises from "@/components/ToolsExercises";
import ToolsSummary from "@/components/ToolsSummary";
import TrialBanner from "@/components/TrialBanner";
import { isSignedIn } from "../api/auth.js";
import { getTrial } from "../api/tools.js";

// The backend's defaults: tools.anon.max-chars and tools.anon.max-pdf for a visitor, tools.max-chars and
// upload.max-size for a signed-in user.
const VISITOR_LIMITS = { chars: 20000, pdfBytes: 5 * 1024 * 1024 };
const USER_LIMITS = { chars: 100000, pdfBytes: 20 * 1024 * 1024 };

// Every panel stays mounted, so a reply that is still being written survives a look at another tab.
const panelClass = "data-[state=inactive]:hidden";

/** `/tools`: summaries, exercises and a chat for anyone. A visitor with no account gets one use, then the gate. */
export default function Tools() {
    const [trial, setTrial] = useState(null);
    const [gateOpen, setGateOpen] = useState(false);
    const [tab, setTab] = useState("summary");

    const refreshTrial = useCallback(() => {
        getTrial()
            .then(setTrial)
            .catch(() => {
                // Not knowing is fine: the backend still decides on each call.
            });
    }, []);

    useEffect(() => {
        refreshTrial();
    }, [refreshTrial]);

    const signedIn = trial ? trial.signedIn : isSignedIn();
    const limits = signedIn ? USER_LIMITS : VISITOR_LIMITS;

    /**
     * Runs one AI tools call. Resolves to `{ value }`, or to `{ gated: true }` when the visitor's free try is
     * spent and the sign-up gate was opened instead. Any other failure is rethrown.
     */
    const attempt = async (call) => {
        if (trial && !trial.signedIn && !trial.trialAvailable) {
            setGateOpen(true);
            return { gated: true };
        }
        try {
            return { value: await call() };
        } catch (err) {
            if (err.code === "TRIAL_USED") {
                setTrial({ signedIn: false, trialAvailable: false });
                setGateOpen(true);
                return { gated: true };
            }
            throw err;
        } finally {
            refreshTrial();
        }
    };

    return (
        <PageContainer className="max-w-screen-md space-y-6">
            <div className="space-y-2">
                <h1 className="text-3xl font-semibold">AI study tools</h1>
                <p className="text-muted-foreground">
                    Summarise a text or a PDF, turn it into exercises with answers, or ask the study assistant a
                    question. Nothing you send here is kept on the server.
                </p>
            </div>

            <TrialBanner trial={trial} />

            <Tabs value={tab} onValueChange={setTab}>
                <TabsList className="h-11">
                    <TabsTrigger value="summary" className="h-9 px-4">
                        Summary
                    </TabsTrigger>
                    <TabsTrigger value="exercises" className="h-9 px-4">
                        Exercises
                    </TabsTrigger>
                    <TabsTrigger value="chat" className="h-9 px-4">
                        Chat
                    </TabsTrigger>
                </TabsList>
                <TabsContent value="summary" forceMount className={panelClass}>
                    <ToolsSummary attempt={attempt} limits={limits} />
                </TabsContent>
                <TabsContent value="exercises" forceMount className={panelClass}>
                    <ToolsExercises attempt={attempt} limits={limits} />
                </TabsContent>
                <TabsContent value="chat" forceMount className={panelClass}>
                    <ChatPanel attempt={attempt} active={tab === "chat"} />
                </TabsContent>
            </Tabs>

            <SignUpGate open={gateOpen} onOpenChange={setGateOpen} />
        </PageContainer>
    );
}
