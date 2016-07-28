package javagi.casestudies.xpath;

public interface XProcessingInstruction extends XNode {
    String getTarget();   // getProcessingInstructionTarget
    String getData();     // getProcessingInstructionData
}