import { GenerationApp } from "@/components/generation-app";
import { ComparisonSection, FooterSection, PipelineSection, TrustSection } from "@/components/landing-sections";
import { SamplePreviewSection } from "@/components/sample-preview-section";

export default function Page() {
  return (
    <>
      <GenerationApp>
        <ComparisonSection />
        <PipelineSection />
        <SamplePreviewSection />
        <TrustSection />
      </GenerationApp>
      <FooterSection />
    </>
  );
}

