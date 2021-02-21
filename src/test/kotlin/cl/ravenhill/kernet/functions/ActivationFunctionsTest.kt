package cl.ravenhill.kernet.functions

import cl.ravenhill.kernet.functions.activation.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.tensorflow.ndarray.FloatNdArray
import org.tensorflow.ndarray.Shape
import org.tensorflow.op.Ops
import org.tensorflow.op.core.Constant
import org.tensorflow.types.TFloat32
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.random.Random

/**
 * @author [Ignacio Slater Muñoz](mailto:ignacio.slater@ug.uchile.cl)
 */
internal class ActivationFunctionsTest {
  private val eps = 1e-3
  private var seed = 0
  private lateinit var rng: Random
  private lateinit var tf: Ops

  @BeforeEach
  fun setUp() {
    tf = Ops.create()
    seed = Random.nextInt()
    rng = Random(seed)
  }

  // region : invariants
  @RepeatedTest(16)
  fun `tanh function result is in range -1 to 1`() {
    val tanh = KTanh<TFloat32>(tf)
    checkActivationFunction(tanh) { _, it ->
      assertTrue(
        it.getFloat() in -1.0..1.0,
        "Test failed with seed: $seed. ${it.getFloat()} is not in [-1, 1]"
      )
    }
  }

  @RepeatedTest(16)
  fun `softmax function result is in range 0 to 1`() {
    val softmax = KSoftmax<TFloat32>(tf)
    checkActivationFunction(softmax) { _, it ->
      assertTrue(
        it.getFloat() in 0.0..1.0,
        "Test failed with seed: $seed. ${it.getFloat()} is not in [0, 1]"
      )
    }
  }
  // endregion

  // region : computations
  @RepeatedTest(16)
  fun `sigmoid results matches function definition`() {
    val sigmoid = KSigmoid<TFloat32>(tf)
    checkActivationFunction(sigmoid) { x, it ->
      val expected = 1 / (1 + exp(-x))
      assertTrue(
        abs(expected - it.getFloat()) < eps,
        "Test failed with seed: $seed. Expected: $expected but got ${it.getFloat()}"
      )
    }
  }

  @RepeatedTest(16)
  fun `ReLU result matches function definition`() {
    val relu = KReLU<TFloat32>(tf)
    checkActivationFunction(relu) { x, it ->
      val expected = max(0F, x)
      assertTrue(
        abs(expected - it.getFloat()) < eps,
        "Test failed with seed: $seed. Expected: $expected but got ${it.getFloat()}"
      )
    }
  }
  //
  @RepeatedTest(16)
  fun `tanh result matches function definition`() {
    val tanh = KTanh<TFloat32>(tf)
    checkActivationFunction(tanh) { x, it ->
      val expected = kotlin.math.tanh(x)
      assertTrue(
        abs(expected - it.getFloat()) < eps,
        "Test failed with seed: $seed. Expected: $expected but got ${it.getFloat()}"
      )
    }
  }

  @Test
  fun `swish result matches function definition`() {
    val swish = KSwish(tf)
    swish.beta = rng.nextFloat()
    checkActivationFunction(swish) { x, it ->
      val expected = x * sigmoid(tf, swish.beta * x)
    }
  }
  // endregion

  private fun checkActivationFunction(
    function: IActivationFunction<TFloat32>,
    lo: Int = 0,
    hi: Int = 1,
    assertFor: (Float, FloatNdArray) -> Unit
  ) {
    val t = randomTensor(rng)
    val result = function(t)
    result.data().scalars().forEachIndexed { index, it -> assertFor(t.data().getFloat(*index), it) }
  }

  private fun checkActivationDerivative(
    function: IActivationFunction<TFloat32>,
    lo: Int = 0,
    hi: Int = 1,
    assertFor: (Float, FloatNdArray) -> Unit
  ) {
    val t = randomTensor(rng)
    val result = function(t)
    result.data().scalars().forEachIndexed { index, it -> assertFor(t.data().getFloat(*index), it) }
  }

  private fun randomTensor(rng: Random, lo: Int = 0, hi: Int = 100): Constant<TFloat32> {
    val shape = LongArray(rng.nextInt(4) + 1) {
      rng.nextLong(1, 10)
    }
    val t = TFloat32.tensorOf(Shape.of(*shape))
    t.data().scalars().forEach { scalar -> scalar.setFloat(hi * (rng.nextFloat() - 1/2) + lo) }
    return tf.constant(t)
  }
}