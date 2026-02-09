from rewrite.python import AutoFormat
from rewrite.test import rewrite_run, python, RecipeSpec


def test_full_formatter():
    rewrite_run(
        # language=python
        python(
            """\
            from typing import Tuple
            from dataclasses import   dataclass
            import os,sys


            @dataclass
            class Test:
                pass


            class FormatTest:

               def __init__( self , x:int,y:int   ):
                   self.x=x
                   self.y    =y
                   self.z=[1,    2,3]
                   self.d={"a":1,"b"    :2}
                   self.test_obj=Test()

               def method_spaces_test(self,a,    b,c):
                   return a+b*c>>2|4

               def multiline_method_one(self, very_long_parameter_name: int,
                       another_long_parameter: str)-> Tuple[int,    str]:
                   return very_long_parameter_name,     another_long_parameter

               def multiline_method_two(
               self,
                   very_long_parameter_name: int,
                       another_long_parameter: str)-> Tuple[int,str]:
                   return very_long_parameter_name,     another_long_parameter

               def list_comprehension_test(self):
                   a = [x    *2 for x in range(10)    if x>5]

                   return [x    *2 for x in range(10)
                        if x>5]

               def dict_comprehension_test(self):
                   keys    =['a','b','c']
                   values=[1,    2,3]
                   return {k:    v**2
                       for k,v in zip(keys,values)
                           if v    %2==0}

               def nested_structures(self):
                   return {"key":[1,2,
                       3],"other":{
                           "nested":4}}

               def operators_test(self):
                   a, b, c,d,   e, f = 1, 2, 3, 4, 5, 6
                   a=1+2
                   b    =3*4
                   c  +=  3
                   d>>=2
                   e|=3
                   f    **=2
                   return f

               def helper_method_1( self,x:int)->int:
                   return x    **2

               def helper_method_2(self,y:int,   z:int )->int:
                   try:
                       result    =y<<2    *z
                       return result
                   except    ZeroDivisionError as e:
                       raise e
                   except(ValueError,    TypeError)    :
                       return 0
                   finally   :
                       self.cleanup(   )

               def cleanup(self):
                   pass

               def conditional_test(self ):
                   x,y,z = 10,8,20
                   a,b,c,d,e,f = 1,2,3,3,4,5

                   if(x>5 and y    <10 or z>=15):
                       result=self.helper_method_1(  x    )
                       return result
                   elif    (a<=b and c    ==d or e    !=f):
                       self.helper_method_2(y,    z)
                       self.nested_structures(  )
                   elif(x&y==4 and z|y    !=0):
                       x>>=1
                       self.method_spaces_test(a,b,    c)

                   while(x**2>=100 and y<<2    <=50):
                       self.operators_test(    )
                       self.test_obj.empty_method(  )

                   for i in range(10) :
                       if i*2>5 and i    %3==0:
                           self.list_comprehension_test(  )


            def top_level_function (x:int,y:int   ):
               return x+y


            class AnotherClass:
               def first_method(self): pass
            """,
            """\
            from typing import Tuple
            from dataclasses import dataclass
            import os, sys


            @dataclass
            class Test:
                pass


            class FormatTest:

                def __init__(self, x: int, y: int):
                    self.x = x
                    self.y = y
                    self.z = [1, 2, 3]
                    self.d = {"a": 1, "b": 2}
                    self.test_obj = Test()

                def method_spaces_test(self, a, b, c):
                    return a + b * c >> 2 | 4

                def multiline_method_one(self, very_long_parameter_name: int,
                                         another_long_parameter: str) -> Tuple[int, str]:
                    return very_long_parameter_name, another_long_parameter

                def multiline_method_two(
                        self,
                        very_long_parameter_name: int,
                        another_long_parameter: str) -> Tuple[int, str]:
                    return very_long_parameter_name, another_long_parameter

                def list_comprehension_test(self):
                    a = [x * 2 for x in range(10) if x > 5]

                    return [x * 2 for x in range(10)
                            if x > 5]

                def dict_comprehension_test(self):
                    keys = ['a', 'b', 'c']
                    values = [1, 2, 3]
                    return {k: v ** 2
                            for k, v in zip(keys, values)
                            if v % 2 == 0}

                def nested_structures(self):
                    return {"key": [1, 2,
                                    3], "other": {
                        "nested": 4}}

                def operators_test(self):
                    a, b, c, d, e, f = 1, 2, 3, 4, 5, 6
                    a = 1 + 2
                    b = 3 * 4
                    c += 3
                    d >>= 2
                    e |= 3
                    f **= 2
                    return f

                def helper_method_1(self, x: int) -> int:
                    return x ** 2

                def helper_method_2(self, y: int, z: int) -> int:
                    try:
                        result = y << 2 * z
                        return result
                    except ZeroDivisionError as e:
                        raise e
                    except (ValueError, TypeError):
                        return 0
                    finally:
                        self.cleanup()

                def cleanup(self):
                    pass

                def conditional_test(self):
                    x, y, z = 10, 8, 20
                    a, b, c, d, e, f = 1, 2, 3, 3, 4, 5

                    if (x > 5 and y < 10 or z >= 15):
                        result = self.helper_method_1(x)
                        return result
                    elif (a <= b and c == d or e != f):
                        self.helper_method_2(y, z)
                        self.nested_structures()
                    elif (x & y == 4 and z | y != 0):
                        x >>= 1
                        self.method_spaces_test(a, b, c)

                    while (x ** 2 >= 100 and y << 2 <= 50):
                        self.operators_test()
                        self.test_obj.empty_method()

                    for i in range(10):
                        if i * 2 > 5 and i % 3 == 0:
                            self.list_comprehension_test()


            def top_level_function(x: int, y: int):
                return x + y


            class AnotherClass:
                def first_method(self): pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )
