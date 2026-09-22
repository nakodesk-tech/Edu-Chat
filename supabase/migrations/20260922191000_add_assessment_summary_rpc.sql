create or replace function public.get_assessment_summaries()
returns table(id uuid,title text,subject text,standard text,duration_minutes integer,status text,total_questions integer,created_by uuid,school_id uuid,created_at timestamptz,published_at timestamptz,shared_student_count bigint,completed_student_count bigint)
language sql stable security definer set search_path = '' as $$
  select a.id,a.title,a.subject,a.standard,a.duration_minutes,a.status,a.total_questions,a.created_by,a.school_id,a.created_at,a.published_at,
    count(distinct case when p.role='student' and p.is_active then gm.user_id end),
    count(distinct case when at.id is not null then at.student_id end)
  from public.assessments a
  left join public.assessment_group_shares s on s.assessment_id=a.id
  left join public.group_members gm on gm.group_id=s.group_id and gm.is_active=true
  left join public.profiles p on p.id=gm.user_id
  left join public.assessment_attempts at on at.assessment_id=a.id and at.group_id=s.group_id and at.student_id=gm.user_id
  where a.created_by=auth.uid() or public.current_user_role()='officer_admin'
    or (public.current_user_role() in ('school_admin','teacher') and a.school_id=public.current_user_school_id())
    or exists(select 1 from public.assessment_group_shares sx join public.group_members gmx on gmx.group_id=sx.group_id where sx.assessment_id=a.id and gmx.user_id=auth.uid() and gmx.is_active=true)
  group by a.id order by a.created_at desc;
$$;
revoke execute on function public.get_assessment_summaries() from public,anon;
grant execute on function public.get_assessment_summaries() to authenticated;
