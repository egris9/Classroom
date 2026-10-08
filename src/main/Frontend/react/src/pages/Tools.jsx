import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import ChatPanel from "@/components/ChatPanel";
import PageContainer from "@/components/PageContainer";
import SignUpGate from "@/components/SignUpGate";
import ToolsExercises from "@/components/ToolsExercises";
import ToolsSummary from "@/components/ToolsSummary";
import TrialBanner from "@/components/TrialBanner";
import { clearSession, getToken, isSignedIn } from "../api/auth.js";
import { getTrial } from "../api/tools.js";

// The backend's defaults: tools.anon.max-chars and tools.anon.max-pdf for a visitor, tools.max-chars and
// upload.max-size for a signed-in user.
const VISITOR_LIMITS = { chars: 20000, pdfBytes: 5 * 1024 * 1024 };
const USER_LIMITS = { chars: 100000, pdfBytes: 20 * 1024 * 1024 };

// Every panel stays mounted, so a reply that is still being written survives a look at another tab.
const panelClass = "data-[state=inactive]:hidden";

/** `/tools`: summaries, exercises and a chat for anyone. A visitor with no account gets one use, then the gate. */
export default function Tools() {
    const navigate = useNavigate();
    const [trial, setTrial] = useState(null);
    const [gateOpen, setGateOpen] = useState(false);
    const [tab, setTab] = useState("summary");

    /** Resolves to what the backend answered, or to null when it could not be asked. */
    const refreshTrial = useCallback(async () => {
        let answer;
        try {
            answer = await getTrial();
        } catch {
            // Not knowing is fine: the backend still decides on each call.
            return null;
        }
        if (!answer.signedIn && getToken()) {
            // The token is no longer accepted and these routes never answer 401, so end the session here:
            // otherwise the header says signed in while the tools treat the person as a visitor.
            clearSession();
            toast.info("Your session has ended. Sign in again to use your account.");
            navigate("/tools", { replace: true });
        }
        setTrial(answer);
        return answer;
    }, [navigate]);

    useEffect(() => {
        refreshTrial();
    }, [refreshTrial]);

    const signedIn = trial ? trial.signedIn : isSignedIn();
    const limits = signedIn ? USER_LIMITS : VISITOR_LIMITS;

    /**
     * Runs one AI tools call. Resolves to `{ value }`, or to `{ gated: true }` when the visitor's free try is
     * spent and the sign-up gate was opened instead. Any other failure is rethrown, with `spentTrial` set when
     * the call that failed was the visitor's free try.
     */
    const attempt = async (call) => {
        if (trial && !trial.signedIn && !trial.trialAvailable) {
            setGateOpen(true);
            return { gated: true };
        }
        try {
            const value = await call();
            refreshTrial();
            return { value };
        } catch (err) {
            if (err.code === "TRIAL_USED") {
                setTrial({ signedIn: false, trialAvailable: false });
                setGateOpen(true);
                return { gated: true };
            }
            const now = await refreshTrial();
            err.spentTrial = Boolean(now && !now.signedIn && !now.trialAvailable);
            throw err;
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
