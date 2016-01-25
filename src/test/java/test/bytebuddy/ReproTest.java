package test.bytebuddy;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.junit.Test;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

public class ReproTest implements AgentBuilder.Listener {

	@Test
	public void test() {
		Instrumentation inst = ByteBuddyAgent.install();

		// Note: using nameStartsWith instead of nameMatches because that's what
		// my application does.

		ClassFileTransformer cft = new AgentBuilder.Default().withListener(this).type(nameStartsWith("test"))
				.transform(new AgentBuilder.Transformer() {

					public Builder<?> transform(Builder<?> builder, TypeDescription typeDescription) {
						return builder.method(isDeclaredBy(typeDescription)).intercept(to(new Object() {

							@RuntimeType
							public void intercept(@SuperCall Callable<?> zuper, @Origin Method method) {
								System.out.println("intercepting " + method.getName());
							}
						}));
					}
				}).installOnByteBuddyAgent();

		try {
			// Call method that will be intercepted.
			MyClass.staticMethod();
		} finally {
			inst.removeTransformer(cft);
		}
	}

	public void onTransformation(TypeDescription typeDescription, DynamicType dynamicType) {
	}

	public void onIgnored(TypeDescription typeDescription) {
	}

	public void onError(String typeName, Throwable throwable) {
		System.out.println(String.format("Error when transforming %s: %s", typeName, throwable.getMessage()));
		throwable.printStackTrace();
	}

	public void onComplete(String typeName) {
	}
}

class MyClass {
	public static void staticMethod() {
		System.out.println("in staticMethod");
	}
}